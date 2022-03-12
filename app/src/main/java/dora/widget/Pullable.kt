package dora.widget

interface Pullable {

    fun canPullDown(): Boolean
    fun canPullUp(): Boolean
}