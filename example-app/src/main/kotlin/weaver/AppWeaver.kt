package example.weaver

import tiny.annotation.WeaverBird
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Provides
import dagger.Module
import javax.inject.Named

@WeaverBird
@Module
class AppWeaver {
	@Provides @Named("big") fun provideBigApple(apple: BigAppleImpl) : Apple {
		return apple
	}

	@Provides @Named("small") fun provideSmallApple(apple: SmallAppleImpl) : Apple {
		return apple
	}

	@Provides @Named("new") fun provideApple() : Apple {
		return SmallAppleImpl()
	}
}

class MyService {
	@Inject @Named("small") lateinit var apple: Apple

	fun getMyApple(): String{
		return apple.howMany()
	}
}

interface Apple {
	fun howMany(): String
}

@Singleton
class BigAppleImpl @Inject constructor(): Apple{
	override fun howMany(): String{
		return "there is only one big apple!"
	}
}

@Singleton
class SmallAppleImpl @Inject constructor(): Apple{
	override fun howMany(): String{
		return "there are five small apples, which one do you want?"
	}
}

