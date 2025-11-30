package com.flowdevproduction.focusguard.di

import android.content.Context
import com.flowdevproduction.focusguard.data.AppPrefs
import com.flowdevproduction.focusguard.data.FocusRepositoryImpl
import com.flowdevproduction.focusguard.domain.FocusRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppPrefs(@ApplicationContext context: Context): AppPrefs {
        return AppPrefs(context)
    }

    @Provides
    @Singleton
    fun provideFocusRepository(
            @ApplicationContext context: Context,
            appPrefs: AppPrefs
    ): FocusRepository {
        return FocusRepositoryImpl(context, appPrefs)
    }
}
