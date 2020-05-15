@file:Suppress("DEPRECATION")

package com.srsssms.covidtracker

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.dialog_layout_success.view.*

class AlertDialogUtility {

    companion object {
        @SuppressLint("InflateParams")
        fun alertDialog(context: Context, alertText: String, animAsset: String) {
            val layoutBuilder = LayoutInflater.from(context).inflate(R.layout.dialog_layout_success, null)
            val builder:AlertDialog.Builder = AlertDialog.Builder(context).setView(layoutBuilder)
            val alertDialog:AlertDialog = builder.show()
            alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_white)
            layoutBuilder.tv_alert.text = alertText
            layoutBuilder.lottie_anim.setAnimation(animAsset)
            layoutBuilder.lottie_anim.loop(true)
            layoutBuilder.lottie_anim.playAnimation()
            layoutBuilder.btn_dismiss.setOnClickListener {
                alertDialog.dismiss()
            }
        }

        @SuppressLint("InflateParams")
        fun withTwoActions(context: Context, dismiss: String, action: String, alertText: String, animAsset: String, function: () -> Unit) {
            val layoutBuilder = LayoutInflater.from(context).inflate(R.layout.dialog_layout_success, null)
            val builder: AlertDialog.Builder = AlertDialog.Builder(context).setView(layoutBuilder)
            val alertDialog: AlertDialog = builder.show()
            alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_white)
            layoutBuilder.tv_alert.text = alertText
            layoutBuilder.lottie_anim.setAnimation(animAsset)
            layoutBuilder.lottie_anim.loop(true)
            layoutBuilder.lottie_anim.playAnimation()
            layoutBuilder.btn_action.visibility = View.VISIBLE
            layoutBuilder.space.visibility = View.VISIBLE
            layoutBuilder.btn_dismiss.text = dismiss
            layoutBuilder.btn_action.text = action
            layoutBuilder.btn_dismiss.setOnClickListener {
                alertDialog.dismiss()
            }
            layoutBuilder.btn_action.setOnClickListener {
                function()
                alertDialog.dismiss()
            }
        }

        @SuppressLint("InflateParams")
        fun withSingleAction(context: Context, dismiss: String, alertText: String, animAsset: String, function: () -> Unit) {
            val layoutBuilder = LayoutInflater.from(context).inflate(R.layout.dialog_layout_success, null)
            val builder:AlertDialog.Builder = AlertDialog.Builder(context).setView(layoutBuilder)
            val alertDialog:AlertDialog = builder.show()
            alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_white)
            layoutBuilder.tv_alert.text = alertText
            layoutBuilder.btn_dismiss.text = dismiss
            layoutBuilder.lottie_anim.setAnimation(animAsset)
            layoutBuilder.lottie_anim.loop(true)
            layoutBuilder.lottie_anim.playAnimation()
            layoutBuilder.btn_dismiss.setOnClickListener {
                function()
            }
        }
    }
}
