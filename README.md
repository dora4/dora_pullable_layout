# DoraPullableLayout

描述：下拉刷新和上拉加载

复杂度：★★★★☆

分组：【Dora大控件组】

关系：暂无

技术要点：事件分发、视图动画、布局容器的布局

### 照片

![avatar](https://github.com/dora4/dora_pullable_layout/blob/main/art/dora_pullable_layout.jpg)

### 动图

![avatar](https://github.com/dora4/dora_pullable_layout/blob/main/art/dora_pullable_layout.gif)

### 软件包

https://github.com/dora4/dora_pullable_layout/blob/main/art/dora_pullable_layout.apk

### 用法

```kotlin
val pullableLayout = findViewById<PullableLayout>(R.id.pullableLayout)
   pullableLayout.setOnRefreshListener(object : PullableLayout.OnRefreshListener {
       override fun onRefresh(layout: PullableLayout) {
           pullableLayout.postDelayed(Runnable { pullableLayout.refreshFinish(PullableLayout.SUCCEED) }, 1000)
       }

       override fun onLoadMore(layout: PullableLayout) {
           pullableLayout.postDelayed(Runnable { pullableLayout.loadMoreFinish(PullableLayout.SUCCEED) }, 1000)
       }
   })
```
