package com.dapm.weatherfit.core.navigation

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dapm.weatherfit.features.extras.FragmentExtras
import com.dapm.weatherfit.features.inicio.FragmentInicio
import com.dapm.weatherfit.features.recomendaciones.FragmentRecomendaciones

class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FragmentInicio()
            1 -> FragmentRecomendaciones()
            2 -> FragmentExtras()
            else -> FragmentInicio()
        }
    }
}
