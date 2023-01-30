package com.akash.flashbrowser.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.fragment.app.Fragment
import com.akash.flashbrowser.R
import com.akash.flashbrowser.activity.MainActivity
import com.akash.flashbrowser.databinding.FragmentBrowseBinding
import java.io.ByteArrayOutputStream

class BrowseFragment(private var urlNew:String) : Fragment() {
    lateinit var binding: FragmentBrowseBinding
    var webicon: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_browse, container, false)
        binding= FragmentBrowseBinding.bind(view)
        return view
    }
    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onResume() {
        super.onResume()
        MainActivity.tabList[MainActivity.myPager.currentItem].name= binding.webview.url.toString()
        MainActivity.tabsBtn.text= MainActivity.tabList.size.toString()
        val mainRef = requireActivity() as MainActivity
        mainRef.binding.refreshBtn.visibility = View.VISIBLE
        mainRef.binding.refreshBtn.setOnClickListener {
            binding.webview.reload()
        }

        binding.webview.apply {
            settings.javaScriptEnabled= true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls= false
            webViewClient= object :WebViewClient(){
                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    if(MainActivity.isDesktopSite)
                        view?.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content',"+" " +
                                "'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));",null)
                }
                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    mainRef.binding.topSearchBar.text= SpannableStringBuilder(url)
                    MainActivity.tabList[MainActivity.myPager.currentItem].name= url.toString()
                    binding.webview.zoomOut()
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    mainRef.binding.progressBar.progress=0
                    mainRef.binding.progressBar.visibility= View.VISIBLE
                    if(url!!.contains("you",ignoreCase = false)) mainRef.binding.root.transitionToEnd()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    mainRef.binding.progressBar.visibility= View.INVISIBLE

                }
            }
            webChromeClient= object: WebChromeClient(){
                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    try {
                        mainRef.binding.webIcon.setImageBitmap(icon)
                        webicon = icon
                        MainActivity.bookmarkIndex= mainRef.isBookmarked(view?.url!!)
                        if(MainActivity.bookmarkIndex!=-1){
                            val array = ByteArrayOutputStream()
                            icon!!.compress(Bitmap.CompressFormat.PNG,100,array)
                            MainActivity.bookmarkList[MainActivity.bookmarkIndex].image= array.toByteArray()
                        }
                    }catch (e:Exception){}
                }
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    super.onShowCustomView(view, callback)
                    binding.webview.visibility= View.GONE
                    binding.customView.visibility= View.VISIBLE
                    binding.customView.addView(view)
                    mainRef.binding.root.transitionToEnd()
                }

                override fun onHideCustomView() {
                    super.onHideCustomView()
                    binding.webview.visibility= View.VISIBLE
                    binding.customView.visibility= View.GONE
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    mainRef.binding.progressBar.progress= newProgress
                }
            }
            when{
                URLUtil.isValidUrl(urlNew)-> loadUrl(urlNew)
                urlNew.contains(".com", ignoreCase = true)-> loadUrl(urlNew)
                else-> loadUrl("https://www.ecosia.org/search?method=index&q=$urlNew")
            }
            binding.webview.setOnTouchListener { _, motionEvent ->
                mainRef.binding.root.onTouchEvent(motionEvent)
                return@setOnTouchListener false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as MainActivity).saveBookmarks()
        //for clearing all webview data
        binding.webview.apply {
            clearFormData()
            clearCache(true)
            clearMatches()
            clearHistory()
            clearSslPreferences()
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
        }
    }
}