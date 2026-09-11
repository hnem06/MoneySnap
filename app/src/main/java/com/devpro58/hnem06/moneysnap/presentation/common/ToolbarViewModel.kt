package com.devpro58.hnem06.moneysnap.presentation.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devpro58.hnem06.moneysnap.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Supplies the signed-in user's avatar to the shared top app bar.
 *
 * Scoped to the activity so all five screens that include the toolbar share one instance and one
 * fetch. Previously every toolbar hardcoded [R.drawable.user_profile_placeholder] and the real
 * [com.devpro58.hnem06.moneysnap.domain.model.AuthUser.photoUrl] was never loaded, so a user who
 * had set an avatar still saw the generic silhouette everywhere except the Profile screen.
 */
@HiltViewModel
class ToolbarViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase
) : ViewModel() {

    private val _avatarUrl = MutableLiveData<String?>(getCurrentUser()?.photoUrl)
    val avatarUrl: LiveData<String?> = _avatarUrl

    /** Re-reads the session; call after the avatar changes so other screens pick it up. */
    fun refresh() {
        _avatarUrl.value = getCurrentUser()?.photoUrl
    }
}
