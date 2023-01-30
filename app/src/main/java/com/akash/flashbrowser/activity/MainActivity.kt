
package com.akash.flashbrowser.activity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.akash.flashbrowser.R
import com.akash.flashbrowser.activity.MainActivity.Companion.myPager
import com.akash.flashbrowser.activity.MainActivity.Companion.tabsBtn
import com.akash.flashbrowser.adapter.TabAdapter
import com.akash.flashbrowser.databinding.ActivityMainBinding
import com.akash.flashbrowser.databinding.BookmarkAddBinding
import com.akash.flashbrowser.databinding.FeaturesBinding
import com.akash.flashbrowser.databinding.TabViewBinding
import com.akash.flashbrowser.fragment.BrowseFragment
import com.akash.flashbrowser.fragment.HomeFragment
import com.akash.flashbrowser.model.Bookmark
import com.akash.flashbrowser.model.Tab
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private var printJob: PrintJob? = null

    companion object {
        var tabList: ArrayList<Tab> = ArrayList()
        private var isFullscreen: Boolean = true
        var isDesktopSite: Boolean = true
        var bookmarkList: ArrayList<Bookmark> = ArrayList()
        var bookmarkIndex: Int = -1
        lateinit var myPager: ViewPager2
        lateinit var tabsBtn: MaterialTextView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getAllBookmarks()
        tabList.add(Tab("Home", HomeFragment()))
        binding.myPager.adapter = TabsAdapter(supportFragmentManager, lifecycle)
        binding.myPager.isUserInputEnabled = false
        myPager=binding.myPager
        tabsBtn= binding.tabBtn
        initializeView()
        changeFullscreen(enable = false)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBackPressed() {
        var frag: BrowseFragment? = null
        try {
            frag = tabList[binding.myPager.currentItem].fragment as BrowseFragment
        } catch (e: Exception) {
        }
        when {
            frag?.binding?.webview?.canGoBack() == true -> frag.binding.webview.goBack()
            binding.myPager.currentItem != 0 -> {
                tabList.removeAt(binding.myPager.currentItem)
                binding.myPager.adapter?.notifyDataSetChanged()
                binding.myPager.currentItem = tabList.size - 1
            }
            else -> super.onBackPressed()
        }
    }

    private inner class TabsAdapter(fa: FragmentManager, lc: Lifecycle) :
        FragmentStateAdapter(fa, lc) {
        override fun getItemCount(): Int = tabList.size

        override fun createFragment(position: Int): Fragment = tabList[position].fragment
    }


    private fun initializeView() {
        binding.tabBtn.setOnClickListener {
            val viewTabs = layoutInflater.inflate(R.layout.tab_view, binding.root, false)
            val tabsBinding = TabViewBinding.bind(viewTabs)
            val dialogtab = MaterialAlertDialogBuilder(this, R.style.roundCornerDialog).setView(viewTabs)
                .setTitle("Select Tab")
                .setPositiveButton("Home"){self, _ ->
                    changeTab(url = "Home",HomeFragment())
                    self.dismiss()}
                .setNeutralButton("Google"){self, _ ->
                    changeTab(url = "Google",BrowseFragment(urlNew = "www.google.com"))
                    self.dismiss()}
                .create()

            tabsBinding.tabsRV.setHasFixedSize(true)
            tabsBinding.tabsRV.layoutManager= LinearLayoutManager(this)
            tabsBinding.tabsRV.adapter= TabAdapter(this, dialogtab)
            dialogtab.show()
            val pbtn = dialogtab.getButton(AlertDialog.BUTTON_POSITIVE)
            val nbtn = dialogtab.getButton(AlertDialog.BUTTON_NEUTRAL)
            pbtn.isAllCaps = false
            nbtn.isAllCaps = false
            pbtn.setTextColor(Color.BLACK)
            nbtn.setTextColor(Color.BLACK)
            pbtn.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ResourcesCompat.getDrawable(resources,R.drawable.ic_baseline_home_24,theme)
                ,null,null,null)
            nbtn.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ResourcesCompat.getDrawable(resources,R.drawable.ic_baseline_add_circle_24,theme)
                ,null,null,null)
        }
        binding.settingBtn.setOnClickListener {
            var frag: BrowseFragment? = null
            try {
                frag = tabList[binding.myPager.currentItem].fragment as BrowseFragment
            } catch (e: Exception) {
            }
            val view = layoutInflater.inflate(R.layout.features, binding.root, false)
            val dialogBinding = FeaturesBinding.bind(view)
            val dialog = MaterialAlertDialogBuilder(this).setView(view).create()
            dialog.window?.apply {
                attributes.gravity = Gravity.BOTTOM
                attributes.y = 50
                setBackgroundDrawable(ColorDrawable(0xFFFFFFFF.toInt()))
            }
            dialog.show()
            if (isFullscreen) {
                dialogBinding.fullscreenbtn.apply {
                    setIconTintResource(R.color.primary)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                }
            }
            frag?.let {
                bookmarkIndex = isBookmarked(it.binding.webview.url!!)
                if (bookmarkIndex != -1) {
                    dialogBinding.fullscreenbtn.apply {
                        setIconTintResource(R.color.primary)
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                    }
                }
            }
            if (isDesktopSite) {
                dialogBinding.desktopbtn.apply {
                    setIconTintResource(R.color.primary)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                }
            }

            dialogBinding.backbtn.setOnClickListener {
                onBackPressed()
            }
            dialogBinding.forwardbtn.setOnClickListener {
                frag?.apply {
                    if (binding.webview.canGoForward())
                        binding.webview.goForward()
                }
            }
            dialogBinding.savebtn.setOnClickListener {
                dialog.dismiss()
                if (frag != null)
                    saveAsPDF(web = frag.binding.webview)
                else Snackbar.make(binding.root, "First Open a Webpage\uD83D\uDE03", 3000).show()
            }
            dialogBinding.fullscreenbtn.setOnClickListener {
                it as MaterialButton
                isFullscreen = if (isFullscreen) {
                    changeFullscreen(enable = false)
                    it.setIconTintResource(R.color.black)
                    it.setTextColor(ContextCompat.getColor(this, R.color.black))
                    false
                } else {
                    changeFullscreen(enable = true)
                    it.setIconTintResource(R.color.primary)
                    it.setTextColor(ContextCompat.getColor(this, R.color.primary))
                    true
                }
            }
            dialogBinding.desktopbtn.setOnClickListener {
                it as MaterialButton
                frag?.binding?.webview?.apply {
                    isDesktopSite = if (isDesktopSite) {
                        settings.userAgentString = null
                        it.setIconTintResource(R.color.black)
                        it.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                        false
                    } else {
                        settings.userAgentString =
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36"
                        settings.useWideViewPort = true
                        evaluateJavascript(
                            "document.querySelector('meta[name=\"viewport\"]').setAttribute('content'," + " " +
                                    "'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));",
                            null
                        )
                        it.setIconTintResource(R.color.primary)
                        it.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                        true
                    }
                    reload()
                    dialog.dismiss()
                }
            }
            dialogBinding.bookmarkbtn.setOnClickListener {
                frag?.let {
                    if (bookmarkIndex == -1) {
                        val viewB =
                            layoutInflater.inflate(R.layout.bookmark_add, binding.root, false)
                        val bBinding = BookmarkAddBinding.bind(viewB)
                        val dialogB = MaterialAlertDialogBuilder(this)
                            .setTitle("Add Bookmark")
                            .setMessage("Url:${it.binding.webview.url}")
                            .setPositiveButton("Add") { self, _ ->
                                try {
                                    val array = ByteArrayOutputStream()
                                    it.webicon?.compress(Bitmap.CompressFormat.PNG,100,array)
                                    bookmarkList.add(
                                        Bookmark(
                                            name = bBinding.bookmarktitle.text.toString(),
                                            url = it.binding.webview.url!!, array.toByteArray()
                                        )
                                    )
                                }catch (e:Exception){
                                    Bookmark(
                                        name = bBinding.bookmarktitle.text.toString(),
                                        url = it.binding.webview.url!!)
                                }
                                self.dismiss()
                            }
                            .setNegativeButton("Cancel") { self, _ -> self.dismiss() }
                            .setView(viewB).create()
                        dialogB.show()
                        bBinding.bookmarktitle.setText(it.binding.webview.title)

                    } else {
                        val dialogB = MaterialAlertDialogBuilder(this)
                            .setTitle("Delete Bookmark")
                            .setMessage("Url:${it.binding.webview.url}")
                            .setPositiveButton("Delete") { self, _ ->
                                bookmarkList.removeAt(bookmarkIndex)
                                self.dismiss()
                            }
                            .setNegativeButton("Cancel") { self, _ -> self.dismiss() }
                            .create()
                            .show()
                    }
                }
                dialog.dismiss()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        printJob?.let {
            when {
                it.isCompleted -> Snackbar.make(
                    binding.root,
                    "Successful -> ${it.info.label}",
                    4000
                ).show()
                it.isFailed -> Snackbar.make(binding.root, "Failed -> ${it.info.label}", 4000)
                    .show()
            }
        }
    }

    private fun saveAsPDF(web: WebView) {
        val pm = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobname = "${URL(web.url).host}_${
            SimpleDateFormat("HH:mm d_MMM_yy", Locale.ENGLISH)
                .format(Calendar.getInstance().time)
        }"
        val printAdapter = web.createPrintDocumentAdapter(jobname)
        val printAttributes = PrintAttributes.Builder()
        printJob = pm.print(jobname, printAdapter, printAttributes.build())

    }

    private fun changeFullscreen(enable: Boolean) {
        if (enable) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, binding.root).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(
                window,
                binding.root
            ).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun isBookmarked(url: String): Int {
        bookmarkList.forEachIndexed { index, bookmark ->
            if (bookmark.url == url) return index
        }
        return -1
    }

    fun saveBookmarks() {
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE).edit()
        val data = GsonBuilder().create().toJson(bookmarkList)
        editor.putString("bookmarkList",data)
        editor.apply()

    }

    fun getAllBookmarks() {
        bookmarkList= ArrayList()
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE)
        val data = editor.getString("bookmarkList",null)
        if (data!= null){
            val list:ArrayList<Bookmark> = GsonBuilder().create().fromJson(data, object :TypeToken<ArrayList<Bookmark>>(){}.type)
            bookmarkList.addAll(list)
        }


    }
}
fun changeTab(url: String, fragment: Fragment) {
    MainActivity.tabList.add(Tab(name = url, fragment= fragment))
    myPager.adapter?.notifyDataSetChanged()
    myPager.currentItem = MainActivity.tabList.size - 1
    tabsBtn.text = MainActivity.tabList.size.toString()
}

fun checkForInternet(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    } else {
        val networkInfo = connectivityManager.activeNetworkInfo ?: return false
        return networkInfo.isConnected
    }
}