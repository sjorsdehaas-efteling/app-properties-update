package com.reproducerapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.facebook.react.ReactApplication
import com.facebook.react.ReactDelegate
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.interfaces.fabric.ReactSurface
import com.facebook.react.internal.featureflags.ReactNativeNewArchitectureFeatureFlags
import com.facebook.react.runtime.ReactSurfaceImpl

class MainFragment : Fragment() {
    private lateinit var reactDelegate: ReactDelegate

    private val reactHost: ReactHost?
        get() = (activity?.application as? ReactApplication)?.reactHost

    private val reactNativeHost: ReactNativeHost?
        get() = (activity?.application as? ReactApplication)?.reactNativeHost

    // We have to keep track of the current props so we can merge them with the new props on an update.
    private var cachedProps = bundleOf()

    val mainComponentName = "ReproducerExample"

    private lateinit var surface: ReactSurface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // In the production version of our app, the mainComponentName, launchOptions and fabricEnabled
        // arguments are provided through the arguments bundle passed to this Fragment
        reactDelegate = if (ReactNativeNewArchitectureFeatureFlags.enableBridgelessArchitecture()) {
            ReactDelegate(requireActivity(), reactHost, mainComponentName, bundleOf())
        } else {
            @Suppress("DEPRECATION")
            ReactDelegate(
                requireActivity(), reactNativeHost, mainComponentName, bundleOf(), false
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (ReactNativeNewArchitectureFeatureFlags.enableBridgelessArchitecture()) {
            surface = reactDelegate.reactHost?.createSurface(
                context = requireActivity(),
                moduleName = mainComponentName,
                cachedProps
            ) as ReactSurface
            reactDelegate.setReactSurface(surface)
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun updateReactProps(newProps: Bundle, resetProps: Boolean) {
        val newArchEnabled = ReactNativeNewArchitectureFeatureFlags.enableBridgelessArchitecture()

        val oldProps =
            if (resetProps) null
            else if (newArchEnabled) cachedProps
            else reactDelegate.reactRootView?.appProperties

        val updatedProperties = merge(oldProps, newProps)

        cachedProps = updatedProperties

        if (newArchEnabled) {
            // In the new architecture, updating the props is only available one the ReactSurfaceImpl, not the ReactSurface interface.
            (surface as? ReactSurfaceImpl)?.updateInitProps(updatedProperties)
        } else {
            // In the old architecture we could update the properties directly on the ReactRootView
            reactDelegate.reactRootView?.appProperties = updatedProperties
        }
    }

    private fun merge(oldProps: Bundle?, newProps: Bundle?) : Bundle {
        val old = oldProps ?: bundleOf()
        val new = newProps ?: bundleOf()

        // Technically an unnecessary step as merged is effectively the same as old
        val merged = Bundle(old)
        merged.putAll(new)
        return merged
    }
}