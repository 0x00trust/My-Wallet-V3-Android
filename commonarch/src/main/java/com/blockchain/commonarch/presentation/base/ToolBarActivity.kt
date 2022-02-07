package com.blockchain.commonarch.presentation.base

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

abstract class ToolBarActivity : AppCompatActivity() {

    /**
     * Applies the title to the [Toolbar] which is then set as the Activity's
     * SupportActionBar.
     *
     * @param toolbar The [Toolbar] for the current activity
     * @param title The title for the page, as a StringRes
     */
    fun setupToolbar(toolbar: Toolbar, @StringRes title: Int) {
        setupToolbar(toolbar, getString(title))
    }

    /**
     * Applies the title to the [Toolbar] which is then set as the Activity's
     * SupportActionBar.
     *
     * @param toolbar The [Toolbar] for the current activity
     * @param title The title for the page, as a String
     */
    fun setupToolbar(toolbar: Toolbar, title: String) {
        toolbar.title = title
        setSupportActionBar(toolbar)
    }
}
