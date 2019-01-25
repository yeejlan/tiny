package example

import tiny.TinyApp
import java.awt.Dimension
import java.awt.Component
import java.awt.EventQueue
import javax.swing.*
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.webapp.WebAppContext
import java.util.prefs.Preferences

private val preferences = Preferences.userRoot().node(SwingJettyControl::class.java.getSimpleName())

class SwingJettyControl(title: String) : JFrame() {
	val w = 380
	val h = 100

	init {
		createUI(title)
	}

	private fun createUI(title: String) {

		setTitle(title)

		val closeBtn = JButton("Reload")
		closeBtn.setMinimumSize(Dimension(180,40))
		closeBtn.addActionListener {
			val location = getLocation()
			preferences.putInt("x", location.x)
			preferences.putInt("y", location.y)
			System.exit(1)
		}

		val gl = GroupLayout(contentPane)
		contentPane.layout = gl
		gl.autoCreateContainerGaps = true
		gl.setHorizontalGroup(gl.createSequentialGroup()
				.addComponent(closeBtn)
		)
		gl.setVerticalGroup(gl.createSequentialGroup()
				.addComponent(closeBtn)
		)
		pack()

		defaultCloseOperation = JFrame.EXIT_ON_CLOSE
		setSize(w, h)
		//setAlwaysOnTop(true)
		setResizable(false)
		val x = preferences.getInt("x", -1)
		val y = preferences.getInt("y", -1)
		val ss = java.awt.Toolkit.getDefaultToolkit().getScreenSize()
		if(x < 0 || y < 0 || x > ss.width-w || y > ss.height-h) {
			setLocationRelativeTo(null)
		}else{
			setLocation(x, y)
		}
		addWindowListener(JFrameCloseHandler())
	}

	inner class JFrameCloseHandler: java.awt.event.WindowAdapter() {
		override fun windowClosing(windowEvent: java.awt.event.WindowEvent){
			val location = getLocation()
			preferences.putInt("x", location.x)
			preferences.putInt("y", location.y)
			System.exit(0)
		}
	}
}

private fun createAndShowGUI() {

	val frame = SwingJettyControl("JettyRun")
	frame.isVisible = true
}

fun runJetty(port: Int = 8080){
	val server = Server(port)
	server.setStopAtShutdown(true)

	val context = WebAppContext()
	context.setContextPath("/")
	context.setResourceBase("./web")
	context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*/classes/.*")
	
	context.setConfigurations(arrayOf(
		org.eclipse.jetty.annotations.AnnotationConfiguration(),
		org.eclipse.jetty.webapp.WebInfConfiguration(), 
		org.eclipse.jetty.webapp.WebXmlConfiguration(),
		org.eclipse.jetty.webapp.MetaInfConfiguration(), 
		org.eclipse.jetty.webapp.FragmentConfiguration(), 
		org.eclipse.jetty.plus.webapp.EnvConfiguration(),
		org.eclipse.jetty.plus.webapp.PlusConfiguration(), 
		org.eclipse.jetty.webapp.JettyWebXmlConfiguration()	
	))	
	server.setHandler(context)
	
	server.start()
}

fun main() {
	val port = System.getProperty("tiny.app.port")?.toIntOrNull() ?: 8080
	runJetty(port)
	EventQueue.invokeLater(::createAndShowGUI)
}
