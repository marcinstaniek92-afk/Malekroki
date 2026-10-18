package com.malekroki.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.malekroki.app.AppContainer
import com.malekroki.app.MaleKrokiApp

/** Creates a ViewModel with access to the app container and the screen's navigation arguments. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as MaleKrokiApp).container
    return viewModel(
        factory = viewModelFactory {
            initializer { create(container, createSavedStateHandle()) }
        },
    )
}
