package tiny.lib

import javax.sql.DataSource

class TinyJDBC() {
	private lateinit var _ds: DataSource
	private var _name: String = ""

	fun setDataSource(dataSource: DataSource) {
		_ds = dataSource
	}

	fun setName(name: String) {
		_name = name
	}


}