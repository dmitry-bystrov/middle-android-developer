package ru.skillbranch.skillarticles.ui.custom.behaviors

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior

class BottombarBehavior<V: View>(context: Context?, attrs: AttributeSet?) :
    HideBottomViewOnScrollBehavior<V>(context, attrs) {
}