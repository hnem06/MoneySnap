package com.devpro58.hnem06.moneysnap.domain.repository

interface OnboardingRepository {
    fun isCompleted(): Boolean
    fun complete()
    fun getLanguageCode(): String?
    fun setLanguageCode(languageCode: String)
}
