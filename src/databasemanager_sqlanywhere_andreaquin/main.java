package databasemanager_sqlanywhere_andreaquin;

import java.sql.SQLException;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * @author Andrea Quin
 */
public class main {
    
    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> {
            // Creando un Splash porque no es necesario un JFrame para la animacion del inicio
            JWindow splash = new JWindow();

            // Logo Animacion
            ImageIcon gif = new ImageIcon(main.class.getResource("/resources/LogoAnimation.gif"));
            JLabel label = new JLabel(gif);
            splash.getContentPane().add(label);

            splash.pack(); 
            splash.setLocationRelativeTo(null);
            splash.setVisible(true);

            // Timer antes del Main Frame
            Timer timer = new Timer(3000, e -> {
                splash.dispose();
                ConnectionManager cm = new ConnectionManager();
                ManagerFrame main = null;
                
                try {
                    ManagerFrame frame = new ManagerFrame(cm);
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
}
