import javax.swing.JFrame
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.geom.Point2D

/**
 * Created by dakue_000 on 20.05.2015.
 */


class Func {
    Func(closure) {
        this.closure = closure
    }

    def closure
}

class App extends JFrame {
    private Func func

    private JPanel panel = new JPanel(new BorderLayout())
    private JPanel canvas

    class Canvas extends JPanel {
        private final ITERATION = 0.01
        private double X0, Y0, MX, MY, XK, XN, MAX
        private Point2D.Double[] points;

        Canvas() {
            initPoints(-Math.PI*(1+0.2), Math.PI*(1+0.2), {x, k ->
                double r = 0
                for (int i = 1; i <= k; ++i)
                    r += func.closure(x, i)
                return r
            })
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g)
            Graphics2D g2d = (Graphics2D)g

            computConsts()
            Polygon p = createPolygon()

            paintLines(g2d)

            g.setColor(Color.BLUE)
            g2d.drawPolyline(p.xpoints, p.ypoints, p.npoints)
        }

        def paintLines(Graphics2D g) {
            g.drawLine(0, (int)Y0, (int)getWidth(), (int)Y0)
            g.drawLine((int)X0, 0, (int)X0, (int)getHeight())

            for (double i = (Math.PI/2)*MX; i <= X0 && i >= ITERATION; i *= 2) {
                g.drawLine((int)X0-i, (int)Y0+7, (int)X0-i, (int)Y0-7)
                g.drawLine((int)i+X0, (int)Y0+7, (int)i+X0, (int)Y0-7)
            }

            int q = 1
            for (double  i = 1*MX; i <= X0; i += MX) {
                g.drawLine((int)X0-i, (int)Y0+5, (int)X0-i, (int)Y0-5)
                g.drawString('-'+q, (int)X0-i-3, (int)Y0+17)
                g.drawLine((int)i+X0, (int)Y0+5, (int)i+X0, (int)Y0-5)
                g.drawString(''+q++, (int)i+X0-3, (int)Y0+17)
            }

            for (double i = (Math.PI/2)*MY; i <= X0 && i >= ITERATION; i *= 2) {
                g.drawLine((int)X0-7, (int)Y0-i, (int)X0+7, (int)Y0-i)
                g.drawLine((int)X0-7, (int)Y0+i, (int)X0-7, (int)Y0+i)
            }

            q = 1
            for (double  i = 1*MY; i <= X0; i += MY) {
                g.drawLine((int)X0-5, (int)Y0-i, (int)X0+5, (int)Y0-i)
                g.drawString(''+q, (int)X0+5, (int)Y0-i+7)
                g.drawLine((int)X0-5, (int)Y0+i, (int)X0+5, (int)Y0+i)
                g.drawString('-'+q++, (int)X0+5, (int)Y0+i+7)
            }
        }

        def createPolygon() {
            Polygon p = new Polygon()

            for (point in points) {
                int x = point.x * MX + X0
                int y = Y0 - point.y * MY
                p.addPoint(x, y)
            }

            return p
        }

        def initPoints(startPoint, endPoint, func) {
            final int count = (endPoint - startPoint) / ITERATION
            points = new Point2D.Double[count]

            XN = startPoint; XK = endPoint; MAX = func(startPoint, 0)

            for (int i = 0; i < count; ++i) {
                points[i] = new Point2D.Double(startPoint, func(startPoint += ITERATION, i))
                if (points[i].y > MAX)
                    MAX = points[i].y
            }
        }

        def computConsts() {
            X0 = getWidth()/2; Y0 = getHeight()/2
            MX = X0/XK*0.9; MY = Y0/MAX*0.9
        }
    }

    App(func) {
        super('Hello groovy')

        this.func = func
        this.canvas = new Canvas()

        panel.add(canvas, BorderLayout.CENTER)
        setContentPane(panel)
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        setBounds(0, 0, 640, 480)
        setVisible(true)
    }
}
func = new Func({x, k ->
    return (Math.pow(-1, k+1)*2/k)*Math.sin(k*x)
})

app = new App(func)