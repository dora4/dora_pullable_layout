package dora.widget

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val pullableLayout = findViewById<PullableLayout>(R.id.pullableLayout)
        pullableLayout.setOnRefreshListener(object : PullableLayout.OnRefreshListener {
            override fun onRefresh(layout: PullableLayout) {
                pullableLayout.postDelayed(Runnable { pullableLayout.refreshFinish(PullableLayout.SUCCEED) }, 1000)
            }

            override fun onLoadMore(layout: PullableLayout) {
                pullableLayout.postDelayed(Runnable { pullableLayout.loadMoreFinish(PullableLayout.SUCCEED) }, 1000)
            }
        })
    }
}