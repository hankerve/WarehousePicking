package com.topsmarteye.warehousepicking.stockList

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.ActivityStockListBinding
import com.topsmarteye.warehousepicking.hideSystemUI


class StockListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockListBinding
    private lateinit var navController: NavController
    lateinit var stockListViewModel: StockListViewModel
    private lateinit var gestureDetector: GestureDetector
    private var isStart = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_stock_list)
        navController = findNavController(R.id.stockListNavHostFragment)
        // The stockListViewModel will be shared between this activity and its fragments
        stockListViewModel = ViewModelProviders.of(this).get(StockListViewModel::class.java)
        binding.stockListViewModel = stockListViewModel

        gestureDetector = GestureDetector(this, FlingGestureListener())

        setupActionBarListener()

    }

    override fun onResume() {
        super.onResume()
        // Enter the sticky immersive mode
        hideSystemUI()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isStart && keyCode == KeyEvent.KEYCODE_STAR) {
            stockListViewModel.onScanTriggeredByKeyboard()
            return true
        } else if (!isStart) {
            when (keyCode) {
                KeyEvent.KEYCODE_1 -> {
                    stockListViewModel.onNext()
                    return true
                }
                KeyEvent.KEYCODE_2 -> {
                    stockListViewModel.onRestock()
                    return true
                }
                KeyEvent.KEYCODE_3 -> {
                    stockListViewModel.onOutOfStock()
                    return true
                }
                KeyEvent.KEYCODE_4 -> {
                    binding.resetOrderButton.requestFocus()
                    stockListViewModel.onResetOrder()
                    return true
                }
                KeyEvent.KEYCODE_5 -> {
                    stockListViewModel.onFinishOrder()
                    return true
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun setupActionBarListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            changeNavItemsVisibility(destination)
        }
        binding.upButton.setOnClickListener {
            onUpButtonPressed()
        }
        stockListViewModel.currentIndex.observe(this, Observer {
            binding.currentIndexTextView.text =
                resources.getString(R.string.current_index_format, it + 1, stockListViewModel.totalItems.value)
        })

    }

    private fun changeNavItemsVisibility(destination: NavDestination) {
        if (destination.id == navController.graph.startDestination) {
            binding.currentIndexTextView.visibility = View.GONE
            binding.resetOrderButton.visibility = View.GONE
            isStart = true
        } else {
            binding.currentIndexTextView.visibility = View.VISIBLE
            binding.resetOrderButton.visibility = View.VISIBLE
            isStart = false
        }
    }

    private fun onUpButtonPressed() {
        if (isStart) {
            finish()
        } else {
            navController.navigateUp()
        }
    }

    private inner class FlingGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            stockListViewModel.onScanTriggeredByKeyboard()
            return true
        }

    }





}
