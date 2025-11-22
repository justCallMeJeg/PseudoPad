package pseudopad.ui.components;

import java.awt.Graphics;
import java.awt.Image;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 *
 * @author Geger John Paul Gabayeron
 */
public class ImagePanel extends JPanel {
    private Image currentImage;
    private String imageResourcePath;
    
    public String getImageResourcePath() {
        return imageResourcePath;
    }
    
    public void setImageResourcePath(String path) {
        // Inside ImagePanel.java setter:
        URL resource = getClass().getResource(path);

        if (resource == null) {
            // This will print the exact path it tried to find.
            System.err.println("CRITICAL: Image resource not found at: " + path);
        }
         
        if (path == null || path.trim().isEmpty()) {
            this.currentImage = null;
            this.imageResourcePath = null;
            repaint();
            return;
        }

        this.imageResourcePath = path;
        
        try {
            // Load the image resource using the updated path
            this.currentImage = new ImageIcon(getClass().getResource(path)).getImage();
        } catch (Exception e) {
            this.currentImage = null; // Display nothing on error
            System.err.println("Failed to load resource path: " + path);
        }
        
        // Force the component to redraw itself with the new image
        revalidate();
        repaint(); 
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Paint the background first
        
        if (currentImage != null) {
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int imageWidth = currentImage.getWidth(this);
            int imageHeight = currentImage.getHeight(this);
            
            // --- SCALING LOGIC ---
            
            // 1. Calculate the scale factor (ensures the image fits within the panel)
            double scaleX = (double) panelWidth / imageWidth;
            double scaleY = (double) panelHeight / imageHeight;
            double scale = Math.min(scaleX, scaleY); 
            
            // 2. Calculate the new dimensions and position to keep it centered
            int scaledWidth = (int) (scale * imageWidth);
            int scaledHeight = (int) (scale * imageHeight);
            int x = (panelWidth - scaledWidth) / 2;
            int y = (panelHeight - scaledHeight) / 2;

            // 3. Draw the scaled image
            g.drawImage(currentImage, x, y, scaledWidth, scaledHeight, this);
        }
    }
}
