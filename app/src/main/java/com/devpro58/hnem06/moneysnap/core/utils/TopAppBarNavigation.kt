package com.devpro58.hnem06.moneysnap.core.utils

import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.presentation.common.ToolbarViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

/** Connects the shared top app bar to the app's existing top-level navigation. */
fun Fragment.setupTopAppBarNavigation(
    root: View,
    @IdRes toolbarId: Int = R.id.topAppBar,
    @IdRes avatarId: Int = R.id.profileAvatar
) {
    val bottomNavigation = requireActivity()
        .findViewById<BottomNavigationView>(R.id.bottomNavigation)

    root.findViewById<MaterialToolbar>(toolbarId).setNavigationOnClickListener { anchor ->
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.main_bottom_nav, menu)
            setOnMenuItemClickListener { item ->
                bottomNavigation.selectedItemId = item.itemId
                true
            }
            show()
        }
    }

    val avatar = root.findViewById<View>(avatarId)
    avatar.setOnClickListener {
        bottomNavigation.selectedItemId = R.id.profileFragment
    }

    // Activity-scoped so every screen sharing this toolbar shows the same avatar, and no
    // fragment needs to know about it.
    if (avatar is ImageView) {
        val toolbarViewModel = ViewModelProvider(requireActivity())[ToolbarViewModel::class.java]
        toolbarViewModel.avatarUrl.observe(viewLifecycleOwner) { url ->
            avatar.load(url) {
                placeholder(R.drawable.user_profile_placeholder)
                error(R.drawable.user_profile_placeholder)
                fallback(R.drawable.user_profile_placeholder)
            }
        }
    }
}
