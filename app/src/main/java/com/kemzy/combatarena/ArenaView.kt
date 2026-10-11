package com.kemzy.combatarena

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class ArenaView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var joyX = 0f
    private var joyY = 0f
    private var moveX = 0f
    private var moveY = 0f
    private var score = 0
    private var shots = 0
    private var hits = 0
    private var message = "Tap FIRE to practise your aim"
    private var crouching = false
    private var aiming = false
    private var gloo = false
    private var lastFire = 0L
    private val targets = mutableListOf<Target>()
    private val buttons = listOf("AIM", "FIRE", "JUMP", "CROUCH", "GLOO")

    private data class Target(var x: Float, var y: Float, var r: Float, var alive: Boolean = true, var drift: Float = 0f)

    init {
        isFocusable = true
        repeat(5) { i -> targets.add(Target(0.16f + i * 0.17f, 0.40f + (i % 2) * 0.12f, 0.035f, true, if (i % 2 == 0) 1f else -1f)) }
    }

    private fun color(c: Int) { p.color = c; p.style = Paint.Style.FILL }
    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, col: Int = 0xFFFFFFFF.toInt()) {
        color(col); p.textSize = size; p.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD); c.drawText(s, x, y, p)
    }
    private fun round(c: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float, col: Int) {
        color(col); c.drawRoundRect(RectF(l,t,r,b), radius, radius, p)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        color(0xFF101C29.toInt()); c.drawRect(0f,0f,w,h,p)
        color(0xFF203B42.toInt()); c.drawRect(0f,h*0.47f,w,h,p)
        color(0xFF172B31.toInt())
        val horizon = h*0.48f
        for (i in 0..12) {
            val x = w*i/12f
            p.strokeWidth = 1.5f; p.style = Paint.Style.STROKE
            c.drawLine(w*0.5f,horizon,x,h,p)
        }
        for (i in 1..7) {
            val y = horizon + (h-horizon)*(i*i/49f)
            c.drawLine(0f,y,w,y,p)
        }
        p.style = Paint.Style.FILL
        text(c,"KÉMZY COMBAT ARENA",w*0.035f,h*0.09f,h*0.045f,0xFFB7FF62.toInt())
        text(c,"TRAINING RANGE  •  PROTOTYPE",w*0.035f,h*0.14f,h*0.025f,0xFFB8C9D4.toInt())
        round(c,w*0.73f,h*0.035f,w*0.965f,h*0.15f,18f,0xCC08121B.toInt())
        text(c,"SCORE  $score",w*0.755f,h*0.085f,h*0.035f,0xFFFFFFFF.toInt())
        text(c,"HITS $hits / SHOTS $shots",w*0.755f,h*0.125f,h*0.025f,0xFFB7FF62.toInt())

        targets.forEach { t ->
            if (t.alive) {
                val tx=t.x*w; val ty=t.y*h; val rr=t.r*h
                color(0xFF111820.toInt()); c.drawOval(tx-rr*0.7f,ty+rr*1.4f,tx+rr*0.7f,ty+rr*3.4f,p)
                color(0xFFE56C58.toInt()); c.drawCircle(tx,ty,rr,p)
                color(0xFFFFD4B8.toInt()); c.drawCircle(tx,ty-rr*0.82f,rr*0.62f,p)
                color(0xFF1B2935.toInt()); c.drawRect(tx-rr*0.8f,ty+rr*0.9f,tx+rr*0.8f,ty+rr*1.9f,p)
                color(0xFFB7FF62.toInt()); p.style=Paint.Style.STROKE; p.strokeWidth=2f
                c.drawCircle(tx,ty,rr*1.35f,p); p.style=Paint.Style.FILL
            }
        }
        // Reticle
        val cx=w*0.53f; val cy=h*0.47f
        color(if (aiming) 0xFFB7FF62.toInt() else 0xFFEAF2F7.toInt()); p.strokeWidth=2.5f; p.style=Paint.Style.STROKE
        val gap=if(aiming) 5f else 10f; val len=if(aiming) 13f else 9f
        c.drawLine(cx-gap-len,cy,cx-gap,cy,p); c.drawLine(cx+gap,cy,cx+gap+len,cy,p)
        c.drawLine(cx,cy-gap-len,cx,cy-gap,p); c.drawLine(cx,cy+gap,cx,cy+gap+len,p)
        c.drawCircle(cx,cy,if(aiming) 24f else 16f,p); p.style=Paint.Style.FILL

        // Player body
        val px=w*0.50f; val py=h*0.78f
        color(0xFF0A1219.toInt()); c.drawOval(px-w*0.045f,py+h*0.075f,px+w*0.045f,py+h*0.10f,p)
        color(0xFFB7FF62.toInt()); c.drawCircle(px,py-h*0.055f,h*0.025f,p)
        color(0xFF273D4B.toInt()); c.drawRoundRect(RectF(px-w*0.028f,py-h*0.03f,px+w*0.028f,py+h*0.06f),10f,10f,p)
        color(0xFFB7FF62.toInt()); c.drawRoundRect(RectF(px+w*0.01f,py-h*0.015f,px+w*0.075f,py+h*0.005f),6f,6f,p)
        if (crouching) text(c,"CROUCH",px-w*0.05f,py+h*0.13f,h*0.022f,0xFFB7FF62.toInt())
        if (gloo) {
            round(c,w*0.40f,h*0.57f,w*0.60f,h*0.74f,18f,0xAA8BE7FF.toInt())
            text(c,"GLOO WALL",w*0.435f,h*0.64f,h*0.024f,0xFFFFFFFF.toInt())
        }

        // Virtual joystick
        color(0x665E7482); p.style=Paint.Style.STROKE; p.strokeWidth=3f
        c.drawCircle(w*0.13f,h*0.77f,h*0.13f,p); p.style=Paint.Style.FILL
        color(0xAA9EB2BF.toInt()); c.drawCircle(w*0.13f+moveX*h*0.075f,h*0.77f+moveY*h*0.075f,h*0.045f,p)
        text(c,"MOVE",w*0.10f,h*0.95f,h*0.023f,0xFFB8C9D4.toInt())

        val bw=w*0.105f; val bh=h*0.105f; val gapB=w*0.012f
        val start=w-(bw*2+gapB)*2.5f
        buttons.forEachIndexed { i, label ->
            val row=if(i<3) 0 else 1
            val col=if(i<3) i else i-3
            val x=start+col*(bw+gapB); val y=if(row==0) h*0.69f else h*0.82f
            val active=(label=="AIM"&&aiming)||(label=="CROUCH"&&crouching)||(label=="GLOO"&&gloo)
            round(c,x,y,x+bw,y+bh,18f,if(active) 0xFFB7FF62.toInt() else if(label=="FIRE") 0xFFE56C58.toInt() else 0xDD182733.toInt())
            text(c,label,x+bw*0.18f,y+bh*0.62f,h*0.027f,if(active) 0xFF14212A.toInt() else 0xFFFFFFFF.toInt())
        }
        round(c,w*0.30f,h*0.92f,w*0.70f,h*0.975f,12f,0xCC08121B.toInt())
        text(c,message,w*0.315f,h*0.955f,h*0.026f,0xFFFFFFFF.toInt())
        postInvalidateDelayed(50)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val w=width.toFloat(); val h=height.toFloat()
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val x=e.getX(i); val y=e.getY(i)
                    if(x<w*0.28f && y>h*0.48f) {
                        val dx=(x-w*0.13f)/(h*0.13f); val dy=(y-h*0.77f)/(h*0.13f)
                        val mag=max(1f, hypot(dx,dy)); moveX=(dx/mag).coerceIn(-1f,1f); moveY=(dy/mag).coerceIn(-1f,1f)
                        joyX=x; joyY=y
                    } else if (x>w*0.70f && y>h*0.62f) {
                        val bw=w*0.105f; val gapB=w*0.012f; val start=w-(bw*2+gapB)*2.5f
                        val idx=(0..4).firstOrNull { j ->
                            val row=if(j<3) 0 else 1; val col=if(j<3) j else j-3
                            val bx=start+col*(bw+gapB); val by=if(row==0) h*0.69f else h*0.82f
                            x in bx..(bx+bw) && y in by..(by+h*0.105f)
                        }
                        when(idx) {
                            0 -> { aiming=true; message="Aim steady — track the target" }
                            1 -> fire()
                            2 -> { message="Jump drill! Combine movement with aim"; }
                            3 -> { crouching=!crouching; message=if(crouching) "Crouch active" else "Standing"; }
                            4 -> { gloo=!gloo; message=if(gloo) "Gloo wall deployed — practise cover" else "Gloo wall removed"; }
                        }
                    } else if(x>w*0.3f) {
                        // Swipe to shift the training reticle and practise tracking.
                        val dx=(x-w*0.53f)/w; val dy=(y-h*0.47f)/h
                        targets.forEach { t -> t.x=(t.x-dx*0.03f).coerceIn(0.06f,0.94f); t.y=(t.y-dy*0.025f).coerceIn(0.28f,0.59f) }
                    }
                }
                invalidate(); return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                moveX=0f; moveY=0f; aiming=false; invalidate(); return true
            }
        }
        return true
    }

    private fun fire() {
        val now=System.currentTimeMillis()
        if(now-lastFire<160) return
        lastFire=now; shots++
        val alive=targets.filter { it.alive }
        if(alive.isNotEmpty()) {
            val t=alive.minByOrNull { kotlin.math.abs(it.x-0.53f)+kotlin.math.abs(it.y-0.47f) }!!
            t.alive=false; hits++; score+=if(aiming) 150 else 100
            message=if(aiming) "HEADSHOT drill hit! +150" else "Target hit! +100"
        } else {
            targets.forEach { it.alive=true; it.x=(0.12f+Math.random().toFloat()*0.76f); it.y=(0.34f+Math.random().toFloat()*0.20f) }
            message="Targets reset — keep practising"
        }
        invalidate()
    }
}
