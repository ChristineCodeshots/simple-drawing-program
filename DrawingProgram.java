package com.mycompany.simpledrawingprogram;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Stack;
import javax.imageio.ImageIO;
import javax.swing.*;

public class SimpleDrawingProgram extends JFrame {
    private DrawingPanel drawingPanel;
    private final JPanel colorDisplayPanel;

    public SimpleDrawingProgram () {
        setTitle("Simple Drawing Program");
        setSize(1920, 1080);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        drawingPanel = new DrawingPanel();
        add(drawingPanel, BorderLayout.CENTER);

        JPanel toolsPanel = new JPanel();

        JButton colorButton = new JButton("Pick Color");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(null, "Choose a Color", drawingPanel.getCurrentColor());
            if (newColor != null) {
                drawingPanel.setCurrentColor(newColor);
                updateColorDisplay(newColor);
            }
        });

        JButton pencilButton = new JButton("Pencil");
        pencilButton.addActionListener(e -> {
            drawingPanel.setEraserMode(false);
            drawingPanel.setBrushSize(2);   
    });

        JButton brushButton = new JButton("Brush");
        brushButton.addActionListener(e -> {
            drawingPanel.setEraserMode(false);
            drawingPanel.setBrushSize(10);
    });
        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> drawingPanel.undo());

        JButton redoButton = new JButton("Redo");
        redoButton.addActionListener(e -> drawingPanel.redo());

        JButton clearButton = new JButton("Clear Canvas");
        clearButton.addActionListener(e -> drawingPanel.clearCanvas());

        JButton saveButton = new JButton("Save Drawing");
        saveButton.addActionListener(e -> drawingPanel.saveImage());

        JButton eraser = new JButton("Eraser");
        eraser.addActionListener(e -> drawingPanel.setEraserMode(true));

        colorDisplayPanel = new JPanel();
        colorDisplayPanel.setPreferredSize(new Dimension(40, 40));
        colorDisplayPanel.setBackground(drawingPanel.getCurrentColor());

        toolsPanel.add(colorButton);
        toolsPanel.add(pencilButton);
        toolsPanel.add(brushButton);
        toolsPanel.add(undoButton);
        toolsPanel.add(redoButton);
        toolsPanel.add(clearButton);
        toolsPanel.add(saveButton);
        toolsPanel.add(eraser);

        add(toolsPanel, BorderLayout.NORTH);
    }

    private void updateColorDisplay(Color color) {
        colorDisplayPanel.setBackground(color);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimpleDrawingProgram app = new SimpleDrawingProgram();
            app.setVisible(true);
        });
    }
}

class DrawingPanel extends JPanel {
    private final BufferedImage canvas;
    private Graphics2D g2d;      
    private int prevX, prevY;
    private boolean isDrawing = false;
    private Color currentColor = Color.BLACK;
    private int brushSize = 5;
    private final Stack<BufferedImage> undoStack = new Stack<>();
    private final Stack<BufferedImage> redoStack = new Stack<>();
    private boolean isEraserMode = false;

    public DrawingPanel() {
        setBackground(new Color(255, 255, 255));
        canvas = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
        g2d = canvas.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g2d.setColor(currentColor);
        g2d.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                saveToUndoStack();
                isDrawing = true;
                prevX = e.getX();
                prevY = e.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDrawing = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDrawing) {
                    int x = e.getX();
                    int y = e.getY();

                    if (isEraserMode) {
                        g2d.setColor(getBackground());
                    } else {
                        g2d.setComposite(AlphaComposite.SrcOver);
                        
                        
                        g2d.setColor(currentColor);
                    }

                    g2d.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine(prevX, prevY, x, y);

                    prevX = x;
                    prevY = y;

                    repaint();
                }
            }
        });
    }

    public void setCurrentColor(Color color) {
        this.currentColor = color;
    }

    public Color getCurrentColor() {
        return currentColor;
    }

    public void setBrushSize(int size) {
        this.brushSize = size;
    }

    public void setEraserMode(boolean enabled) {
        isEraserMode = enabled;
        if (enabled) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public void clearCanvas() {
        saveToUndoStack();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(currentColor);
        repaint();
    }
   
    
    public void saveToUndoStack() {
        BufferedImage copy = new BufferedImage(canvas.getWidth(), canvas.getHeight(), canvas.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(canvas, 0, 0, null);
        undoStack.push(copy);
        redoStack.clear();    
        
        
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(copyCanvas());
            BufferedImage previous = undoStack.pop();
            g2d.drawImage(previous, 0, 0, null);
            repaint();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(copyCanvas());
            BufferedImage next = redoStack.pop();
            g2d.drawImage(next, 0, 0, null);
            repaint();
        }
    }

    private BufferedImage copyCanvas() {
        BufferedImage copy = new BufferedImage(canvas.getWidth(), canvas.getHeight(), canvas.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(canvas, 0, 0, null);
        return copy;
    }

    public void saveImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Image");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Image", "png"));
        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            if (!fileToSave.getName().toLowerCase().endsWith(".png")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".png");
            }

            try {
                ImageIO.write(canvas, "PNG", fileToSave);
                JOptionPane.showMessageDialog(this, "Image saved successfully at " + fileToSave.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving image! " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(canvas, 0, 0, null);
 
    }
}