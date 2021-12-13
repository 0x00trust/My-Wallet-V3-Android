package com.blockchain.componentlib.button

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.image.ImageResource

abstract class BaseSplitButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var onPrimaryButtonClick by mutableStateOf({})
    var primaryButtonText by mutableStateOf("")
    var primaryButtonState by mutableStateOf(ButtonState.Enabled)
    var startButtonIcon: ImageResource by mutableStateOf(ImageResource.None)

    var onSecondaryButtonClick by mutableStateOf({})
    var secondaryButtonText by mutableStateOf("")
    var secondaryButtonState by mutableStateOf(ButtonState.Enabled)
    var endButtonIcon: ImageResource by mutableStateOf(ImageResource.None)

    var primaryButtonAlignment by mutableStateOf(Alignment.START)

    fun clearState() {
        onPrimaryButtonClick = {}
        primaryButtonText = ""
        primaryButtonState = ButtonState.Enabled
        startButtonIcon = ImageResource.None

        onSecondaryButtonClick = {}
        secondaryButtonText = ""
        secondaryButtonState = ButtonState.Enabled
        endButtonIcon = ImageResource.None

        primaryButtonAlignment = Alignment.START
    }
}

enum class Alignment {
    END,
    START
}
