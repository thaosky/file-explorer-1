import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                FileExplorer fileExplorer = new FileExplorer();

                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


                frame.setContentPane(fileExplorer.getGui());
                frame.pack();
                frame.setMinimumSize(new Dimension(1000, 420));
                frame.setVisible(true);

                fileExplorer.showRootFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
