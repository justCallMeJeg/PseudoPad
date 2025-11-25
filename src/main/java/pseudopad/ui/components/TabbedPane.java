package pseudopad.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import pseudopad.utils.IconManager;

/**
 * 
 * * @author Geger John Paul Gabayeron
 */
public class TabbedPane extends JTabbedPane {
    private boolean dragging = false;
    private int draggedTabIndex = -1;
    private int targetTabIndex = -1; 
    private BufferedImage tabImage = null;

    private JPanel headerToolbar;
    private JPanel leftButtonPanel;
    private JPanel rightButtonPanel;

    public TabbedPane() {
        super();
        
        // 1. Setup FlatLaf
        this.putClientProperty(FlatClientProperties.TABBED_PANE_SHOW_TAB_SEPARATORS, true);
        this.putClientProperty(FlatClientProperties.TABBED_PANE_SCROLL_BUTTONS_POLICY, "asNeeded");
        this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // 2. Handle Close Logic
        this.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK,
                (BiConsumer<JTabbedPane, Integer>) (tabPane, tabIndex) -> {
                    tabPane.removeTabAt(tabIndex);
                }
        );

        this.addChangeListener(e -> updateCloseButtons());

        // 3. Initialize Features
        initDragDrop();
        initHeaderToolbar();
    }
    
    private void initHeaderToolbar() {
        // Use BorderLayout to strictly separate Left and Right components
        headerToolbar = new JPanel(new BorderLayout());
        headerToolbar.setOpaque(false);
        headerToolbar.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));

        // Initialize Sub-panels with BoxLayout for horizontal stacking
        leftButtonPanel = new JPanel();
        leftButtonPanel.setLayout(new BoxLayout(leftButtonPanel, BoxLayout.LINE_AXIS));
        leftButtonPanel.setOpaque(false);
        
        rightButtonPanel = new JPanel();
        rightButtonPanel.setLayout(new BoxLayout(rightButtonPanel, BoxLayout.LINE_AXIS));
        rightButtonPanel.setOpaque(false);

        // Add them to the West and East regions
        headerToolbar.add(leftButtonPanel, BorderLayout.WEST);
        headerToolbar.add(rightButtonPanel, BorderLayout.EAST);

        // Register as the Trailing Component
        this.putClientProperty(FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT, headerToolbar);
    }

    private void styleHeaderButton(JButton button) {
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.setFocusable(false);
        button.setMargin(new Insets(2, 2, 2, 2));
        // Force square size
        button.setPreferredSize(new Dimension(24, 24));
        button.setMaximumSize(new Dimension(24, 24));
    }

    public void addLeftHeaderButton(JButton button) {
        styleHeaderButton(button);
        leftButtonPanel.add(button);
        leftButtonPanel.add(Box.createHorizontalStrut(2)); // Gap
        headerToolbar.revalidate();
        headerToolbar.repaint();
    }

    public void addRightHeaderButton(JButton button) {
        styleHeaderButton(button);
        // Add gap before the button to separate it from neighbors
        rightButtonPanel.add(Box.createHorizontalStrut(2)); 
        rightButtonPanel.add(button);
        headerToolbar.revalidate();
        headerToolbar.repaint();
    }

    public void setMinimizeAction(Action action) {
        JButton minBtn = new JButton(action);
        
        // If action has no icon, use fallback text
        if (minBtn.getIcon() == null) {
             minBtn.setText("-"); 
        } else {
             minBtn.setText(""); // Clear text if icon exists
        }
        
        minBtn.setToolTipText("Minimize View");
        addRightHeaderButton(minBtn);
    }

    public void setMinimizeAction(ActionListener listener) {
        JButton minBtn = new JButton();
        if (IconManager.get("minimize") != null) {
             minBtn.setIcon(IconManager.get("minimize"));
        } else {
             minBtn.setText("—"); 
        }
        minBtn.setToolTipText("Minimize View");
        minBtn.addActionListener(listener);
        addRightHeaderButton(minBtn);
    }

    private void updateCloseButtons() {
        int selectedIndex = getSelectedIndex();
        int tabCount = getTabCount();
        for (int i = 0; i < tabCount; i++) {
            Component c = getComponentAt(i);
            if (c instanceof JComponent jComponent) {
                boolean isSelected = (i == selectedIndex);
                jComponent.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, isSelected);
            }
        }
    }

    @Override
    public void addTab(String title, Component component) {
        super.addTab(title, component);
        updateCloseButtons();
    }

    private void initDragDrop() {
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                draggedTabIndex = indexAtLocation(e.getX(), e.getY());
                if (draggedTabIndex != -1) {
                    dragging = true;
                    Rectangle bounds = getBoundsAt(draggedTabIndex);
                    tabImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = tabImage.createGraphics();
                    paint(g2);
                    g2.dispose();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragging || draggedTabIndex == -1) return;
                int hoverIndex = indexAtLocation(e.getX(), e.getY());
                if (hoverIndex != -1) {
                    targetTabIndex = hoverIndex;
                } else {
                    if (getTabCount() > 0) {
                        Rectangle lastTab = getBoundsAt(getTabCount() - 1);
                        if (e.getX() > lastTab.x + lastTab.width) {
                            targetTabIndex = getTabCount(); 
                        }
                    }
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragging && draggedTabIndex != -1 && targetTabIndex != -1 && targetTabIndex != draggedTabIndex) {
                    moveTab(draggedTabIndex, targetTabIndex);
                }
                dragging = false;
                draggedTabIndex = -1;
                targetTabIndex = -1;
                tabImage = null;
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void moveTab(int src, int dst) {
        if (dst >= getTabCount()) dst = getTabCount() - 1;
        Component comp = getComponentAt(src);
        String title = getTitleAt(src);
        Icon icon = getIconAt(src);
        String tip = getToolTipTextAt(src);
        boolean isEnabled = isEnabledAt(src);
        removeTabAt(src);
        insertTab(title, icon, comp, tip, dst);
        setEnabledAt(dst, isEnabled);
        setSelectedIndex(dst);
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (dragging && targetTabIndex != -1) {
            Graphics2D g2 = (Graphics2D) g.create();
            Rectangle bounds;
            int x;
            if (targetTabIndex < getTabCount()) {
                bounds = getBoundsAt(targetTabIndex);
                x = bounds.x;
            } else {
                bounds = getBoundsAt(getTabCount() - 1);
                x = bounds.x + bounds.width;
            }
            g2.setColor(new Color(0, 153, 204)); 
            g2.fillRect(x - 2, bounds.y, 4, bounds.height); 
            g2.dispose();
        }
    }
}
