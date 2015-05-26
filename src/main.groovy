import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JTextField
import java.awt.AlphaComposite
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridLayout
import java.awt.Polygon
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.geom.Point2D
import javax.swing.Timer
import java.awt.image.BufferedImage
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by dakue_000 on 20.05.2015.
 */

class Parser {
    private vars = new String[2]
    private func = null

    private rpn = new RPN()
    private class RPN {
        Deque<String> deque = new ArrayDeque<String>()
        List<String> list = new ArrayList<String>()

    }

    private class Token {
        String str
        Token(str) {
            this.str = str
        }
    }

    static private class Patterns {
        static Pattern body = ~/^y ?= ?(.*)/
        static Pattern plus = ~/^(.{1,}?) ?([+-].*)/
        static Pattern mul = ~/^(.{1,}?) ?([*\\/].*)/
    }

    Parser(str) {
        Matcher matcher = str =~ Patterns.body; matcher.find()
        str = matcher.group(1)

        def plusTokens = parse(str)
        println plusTokens
        for (def token in plusTokens) {
            def mulTokens = parse(token, Patterns.mul)

            println '    ' + mulTokens
            for (def mulToken in mulTokens) {

            }
        }

        println plusTokens
    }

    def getFunc() { return func }

    private parse(str, pattern = Patterns.plus) {
        def strs = []

        try {
            for (boolean needNextIteration = true; needNextIteration && str.size(); ) {
                def matcher = str =~ pattern; matcher.find()

                if (matcher.groupCount() == 2) {
                    str = matcher.group(2)
//                    strs << matcher.group(2) + matcher.group(3)
                    strs << matcher.group(1)
                }
//                else {
//                    needNextIteration = false
//                    println 'needNextIteration = false'
//                }
            }
        } catch (IllegalStateException e) {
            strs << str
        }

        return strs
    }

    private createFunc(Matcher matcher) {
        Double d = 1
        try {
            d = Double.parseDouble(matcher.group(2))
        } catch (Exception e) {}

        if (matcher.group(3) == 'x') {
            return {x -> d*x}
        } else {
            throw new Exception('Parse error')
        }
    }
}

class Func {
    static final double ITERATION = 0.01
    private double MAX, startPoint, endPoint

    private int currentAccuracy = 0, maxAccuracy = 10000
    private List<Point2D.Double[]> pointsList = []
    private List<Double> maxList = []

    def getXN() { return startPoint }
    def getXK() { return endPoint }
    def getMAX() { return maxList[maxList.size()-1] }
    def getPoints(k) { return pointsList[k-1] }
    def accuracy() { return currentAccuracy }
    def maxAccuracy() { return maxAccuracy }

    Func(closure, startPoint, endPoint) {
        this.closure = closure
        this.startPoint = startPoint
        this.endPoint = endPoint

        initPoints(1)
    }

    def initPoints(k) {
        assert(k>0)
        --k

        if (pointsList.size() == k+1 || pointsList.size() >= maxAccuracy)
            return

        if ( (k-pointsList.size()) >= 1)
            initPoints(k-1)

        final int count = (endPoint - startPoint) / ITERATION
        def points = new Point2D.Double[count]
        def prevPoints = (k > 0) ? pointsList[k-1] : null
        def start = startPoint

        MAX = closure(start, k+1)

        for (int i = 0; i < count; ++i) {
            double prevFuncValue = prevPoints ?  prevPoints[i].y : 0
            points[i] = new Point2D.Double(start, closure(start, k+1) + prevFuncValue)
            if (points[i].y > MAX)
                MAX = points[i].y
            start += ITERATION
        }

        pointsList[k] = points
        maxList[k] = MAX
        currentAccuracy = pointsList.size()
    }

    /**
     *
     * @param f ( Point(double, double) )
     */
    def forEach(f, k = pointsList.size()) {
        for (point in getPoints(k)) {
            f(point)
        }
    }

    def closure
}



class App extends JFrame {
    private Func func

    private JPanel panel = new JPanel(new BorderLayout())
    private JTextField  textField = new JTextField()
    private JButton btn = new JButton('Button')
    private JPanel upPanel = new JPanel(new GridLayout(1, 2)) {{
        add(textField); add(btn)
    }}

    private Canvas canvas

    private Timer timer = new Timer(100, new ActionListener() {
        private int k = 1
        @Override
        void actionPerformed(ActionEvent e) {
            func.initPoints(k++)
            canvas.repaint()

            def time = timer.getDelay()
            timer.setDelay( (int) (time > 10) ? time*0.9 : 10 )
            if (k > 5000)
                timer.stop()
        }
    })

    class Canvas extends JPanel {
        private static final ITERATION = 0.01
        private double X0, Y0, MX, MY

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g)
            Graphics2D g2d = (Graphics2D)g
            setupRender(g2d)
            computConsts()

            paintLines(g2d)
            drawFuncPolygon(g2d)

            drawBorder(g2d)
        }

        private def setupRender(g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }

        private def paintLines(Graphics2D g) {
            g.drawLine(0, (int)Y0, (int)getWidth(), (int)Y0)
            g.drawLine((int)X0, 0, (int)X0, (int)getHeight())

            // Pi
            final def pis = ['\u00BD\u03C0', '\u03C0', '\u00BE\u03C0', '2\u03C0']; int q = 0
            final drawPoint = {x1, y1, x2, y2, text, offsetx, offsety ->
                g.drawLine((int)x1, (int)y1, (int)x2, (int)y2)
                g.drawString(text, (int)x1+offsetx, (int)y1+offsety)
            }

            // draw Ox
            q = 0
            for (double i = (Math.PI/2)*MX; i <= X0 && i >= ITERATION; i *= 2) {
                drawPoint(i+X0, Y0+8, i+X0, Y0-8, pis[q], -5, 12)
                drawPoint(X0-i, Y0+8, X0-i, Y0-8, '-'+pis[q], -5, 12)
                if (pis.size() == q++)
                    q = 0
            }

            q = 1
            for (double  i = 1*MX; i <= X0; i += MX) {
                drawPoint(X0+i, Y0+5, X0+i, Y0-5, ''+q, -5, 12)
                drawPoint(X0-i, Y0+5, X0-i, Y0-5, '-'+q++, -5, 12)
            }

            // draw Oy
            q = 0
            for (double i = (Math.PI/2)*MY; i <= X0 && i >= ITERATION; i *= 2) {
                drawPoint(X0+8, Y0-i, X0-8, Y0-i, pis[q], 3, 5)
                drawPoint(X0-8, Y0+i, X0+8, Y0+i, '-'+pis[q], 17, 5)
                if (pis.size() == q++)
                    q = 0
            }

            q = 1
            for (double  i = 1*MY; i <= X0; i += MY) {
                drawPoint(X0-5, Y0-i, X0+5, Y0-i, ''+q, 13, 8)
                drawPoint(X0-5, Y0+i, X0+5, Y0+i, '-'+q++, 10, 8)
            }
        }

        private def drawFuncPolygon(g) {
            def color = new Color(0,0,255, 255)
            int acc = func.accuracy(), end = (func.maxAccuracy() >= 10) ? 10 : func.maxAccuracy()

            for (int i = 1; i <= end; ++i) {
                int k = (int) 1.0/end *i*acc
                if (!k)
                    continue

                Polygon p = new Polygon()

                g.setColor(new Color(0,0,255, (int)1.0/end*i*255))

                func.forEach({ point ->
                    int x = point.x * MX + X0
                    int y = Y0 - point.y * MY
                    p.addPoint(x, y)
                }, k)

                g.drawPolyline(p.xpoints, p.ypoints, p.npoints)
            }
        }

        private def drawBorder(g) {
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.DST_OUT, 1.0)
            BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB)
            Graphics2D gImg = img.createGraphics()
            setupRender(gImg)

            gImg.setPaint(Color.DARK_GRAY)
            gImg.fillRect(0, 0, getWidth(), getHeight())

            gImg.setComposite(ac)

            gImg.setPaint(Color.WHITE)
            gImg.fillRoundRect(4, 5, getWidth()-8, getHeight()-10, 5, 5)

            g.drawImage(img, 0, 0, null)
            gImg.dispose()
        }

        private def computConsts() {
            X0 = getWidth()/2; Y0 = getHeight()/2
            MX = X0 / func.getXK() * 0.9; MY = Y0 / func.getMAX() * 0.9
        }
    }

    App(func) {
        super('Hello groovy')

        this.func = func
        this.canvas = new Canvas()
        this.timer.start()

        panel.add(upPanel, BorderLayout.NORTH)
        panel.add(canvas, BorderLayout.CENTER)

//        upPanel.add(textField)
        setContentPane(panel)
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        setBounds(0, 0, 640, 480)
        setVisible(true)
    }
}

parser = new Parser('y = -3*x - 5/x + 7/x + 9*x*2 - 11 + 13*x + 15 + 17 - 19/x')
parser2 = new Parser('y = -x*32')


//println 'Parser text: f(5): ' + parser.getFunc()(5)
//
//func = new Func({x, k ->
//    return (Math.pow(-1, k+1)*2/k)*Math.sin(k*x)
//}, -Math.PI*1.2, Math.PI*1.2)
//
////func = new Func( {x, k ->
////    return parser.getFunc()(x) }, -Math.PI*1.2, Math.PI*1.2)
//
//app = new App(func)
