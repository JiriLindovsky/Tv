package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.TvAppDatabase
import com.example.data.TvAppRepository
import com.example.ui.TvAppViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var db: TvAppDatabase
  private lateinit var repository: TvAppRepository
  private lateinit var viewModel: TvAppViewModel

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, TvAppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = TvAppRepository(db.tvAppDao())
    viewModel = TvAppViewModel(repository)
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Minimalist TV", appName)
  }

  @Test
  fun `diagnose complicated view crash`() {
    // Render TvLauncherScreen
    composeTestRule.setContent {
      TvLauncherScreen(viewModel = viewModel)
    }

    // Capture initial state and perform click to transition to Complicated View
    composeTestRule.onNodeWithTag("switch_to_complicated_view").performClick()
    
    // Wait for compose to settle
    composeTestRule.waitForIdle()
  }
}
