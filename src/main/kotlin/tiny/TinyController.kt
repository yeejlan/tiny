package tiny

class TinyController{

	var view = TinyView()

	fun before(){
		//pass
	}

	fun after(){
		//pass
	}

	/**
	* render a template to string(without .tpl)
	**/
	fun render(tplPath : String): String{
		return view.render(tplPath)
	}

	fun callAction(path: String){

	}
}