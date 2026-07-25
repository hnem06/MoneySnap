package com.devpro58.hnem06.moneysnap.core.utils

import android.view.View
import android.widget.PopupMenu
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.devpro58.hnem06.moneysnap.R
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

    root.findViewById<View>(avatarId).setOnClickListener {
        bottomNavigation.selectedItemId = R.id.profileFragment
    }
}
