package example

import tiny.TinyApp
import java.awt.Dimension
import java.awt.Component
import java.awt.EventQueue
import javax.swing.*
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.webapp.WebAppContext

private lateinit var jettyServer: Server
private lateinit var jettyWebApp: WebAppContext

class SwingJettyControl(title: String) : JFrame() {

	init {
		createUI(title)
	}

	private fun createUI(title: String) {

		setTitle(title)

		val closeBtn = JButton("Stop Jetty")
		closeBtn.setMinimumSize(Dimension(180,40))
		closeBtn.addActionListener { System.exit(0) }

		createLayout(closeBtn)

		defaultCloseOperation = JFrame.EXIT_ON_CLOSE
		setSize(380, 100)
		setAlwaysOnTop(true)
		setResizable(false)
		setLocationRelativeTo(null)
	}


	private fun createLayout(vararg arg: JComponent) {
		val gl = GroupLayout(contentPane)
		contentPane.layout = gl
		gl.autoCreateContainerGaps = true
		gl.setHorizontalGroup(gl.createSequentialGroup()
				.addComponent(arg[0])
		)
		gl.setVerticalGroup(gl.createSequentialGroup()
				.addComponent(arg[0])
		)
		pack()
	}
}

private fun createAndShowGUI() {

	val frame = SwingJettyControl("JettyRun")
	frame.isVisible = true
}

fun runJetty(port: Int = 8080){
	val server = Server(port)
	jettyServer = server
	server.setStopAtShutdown(true)

	val context = WebAppContext()
	jettyWebApp = context
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
	runJetty()
	EventQueue.invokeLater(::createAndShowGUI)
	jettyServer.join()
}
