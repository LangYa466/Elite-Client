package com.instrumentalist.elite.hacks.features.movement

import com.instrumentalist.elite.events.features.MotionEvent
import com.instrumentalist.elite.events.features.SendPacketEvent
import com.instrumentalist.elite.events.features.TickEvent
import com.instrumentalist.elite.events.features.UpdateEvent
import com.instrumentalist.elite.hacks.Module
import com.instrumentalist.elite.hacks.ModuleCategory
import com.instrumentalist.elite.hacks.ModuleManager
import com.instrumentalist.elite.utils.ChatUtil
import com.instrumentalist.elite.utils.IMinecraft
import com.instrumentalist.elite.utils.move.MovementUtil
import com.instrumentalist.elite.utils.packet.PacketUtil
import com.instrumentalist.elite.utils.rotation.RotationUtil
import com.instrumentalist.elite.utils.value.BooleanValue
import com.instrumentalist.elite.utils.value.ListValue
import net.minecraft.client.input.Input
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.BowItem
import net.minecraft.item.Items
import net.minecraft.item.PotionItem
import net.minecraft.item.SwordItem
import net.minecraft.item.consume.UseAction
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.util.Hand
import org.lwjgl.glfw.GLFW

class NoSlow : Module("No Slow", ModuleCategory.Movement, GLFW.GLFW_KEY_UNKNOWN, false, true) {
    companion object {
        private val mode = ListValue("Mode", arrayOf("Vanilla", "Hypixel"), "Vanilla")

        private val sneak = BooleanValue("Sneak", false) { mode.get().equals("vanilla", true) }

        fun noSlowHook(): Boolean {
            if (ModuleManager.getModuleState(NoSlow())) {
                val nonSneakFast = IMinecraft.mc.player!!.activeItem.useAction == UseAction.SPYGLASS || IMinecraft.mc.player!!.activeItem.useAction == UseAction.TOOT_HORN || IMinecraft.mc.player!!.activeItem.useAction == UseAction.BUNDLE || IMinecraft.mc.player!!.activeItem.useAction == UseAction.DRINK || IMinecraft.mc.player!!.activeItem.useAction == UseAction.BRUSH || IMinecraft.mc.player!!.activeItem.useAction == UseAction.BLOCK || IMinecraft.mc.player!!.activeItem.useAction == UseAction.EAT || IMinecraft.mc.player!!.activeItem.useAction == UseAction.CROSSBOW || IMinecraft.mc.player!!.activeItem.useAction == UseAction.SPEAR || IMinecraft.mc.player!!.activeItem.useAction == UseAction.BOW
                when (mode.get().lowercase()) {
                    "vanilla" -> return sneak.get() && IMinecraft.mc.player!!.isSneaking || nonSneakFast
                    "hypixel" -> return IMinecraft.mc.player!!.mainHandStack.item !is SwordItem && !IMinecraft.mc.player!!.isSneaking && nonSneakFast
                }
            }

            return false
        }
    }

    private var waitingPacket = false
    private var waitJump = false

    override fun tag(): String {
        return mode.get()
    }

    override fun onDisable() {
        waitingPacket = false
    }

    override fun onEnable() {}

    override fun onMotion(event: MotionEvent) {
        if (IMinecraft.mc.player == null) return

        if (mode.get().equals("hypixel", true) && IMinecraft.mc.player!!.mainHandStack.item !is SwordItem && IMinecraft.mc.player!!.isUsingItem && IMinecraft.mc.player!!.isOnGround)
            event.y += 1E-14

        if (waitJump && IMinecraft.mc.player!!.isOnGround) {
            IMinecraft.mc.options.jumpKey.isPressed = false
            IMinecraft.mc.player!!.jump()
            waitJump = false
        } else if (event.y > 0.2) {
            waitJump = false
        }
    }

    override fun onSendPacket(event: SendPacketEvent) {
        if (IMinecraft.mc.player == null) return

        val packet = event.packet

        if (mode.get().equals("hypixel", true) && MovementUtil.fallTicks < 2 && (IMinecraft.mc.player!!.mainHandStack.item.components.contains(DataComponentTypes.FOOD) || IMinecraft.mc.player!!.mainHandStack.item == Items.POTION || IMinecraft.mc.player!!.mainHandStack.item is BowItem || IMinecraft.mc.player!!.mainHandStack.item == Items.MILK_BUCKET)) {
            if (packet is PlayerInteractItemC2SPacket) {
                event.cancel()

                if (IMinecraft.mc.player!!.isOnGround) {
                    IMinecraft.mc.options.jumpKey.isPressed = false
                    IMinecraft.mc.player!!.jump()
                } else {
                    waitJump = true
                }

                waitingPacket = true
            }

            if (packet is PlayerInteractBlockC2SPacket)
                event.cancel()
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        if (IMinecraft.mc.player == null) return

        if (mode.get().equals("hypixel", true) && waitingPacket && !IMinecraft.mc.player!!.isUsingItem)
            waitingPacket = false

        if (mode.get().equals("hypixel", true) && waitingPacket && MovementUtil.fallTicks >= 2 && (IMinecraft.mc.player!!.mainHandStack.item.components.contains(DataComponentTypes.FOOD) || IMinecraft.mc.player!!.mainHandStack.item == Items.POTION || IMinecraft.mc.player!!.mainHandStack.item is BowItem || IMinecraft.mc.player!!.mainHandStack.item == Items.MILK_BUCKET)) {
            val yaw = if (RotationUtil.currentYaw != null) RotationUtil.currentYaw else IMinecraft.mc.player!!.yaw
            val pitch = if (RotationUtil.currentPitch != null) RotationUtil.currentPitch else IMinecraft.mc.player!!.pitch

            PacketUtil.sendPacket(
                PlayerInteractItemC2SPacket(
                    Hand.MAIN_HAND,
                    0,
                    yaw!!,
                    pitch!!
                )
            )

            waitingPacket = false
        }
    }

    override fun onTick(event: TickEvent) {
        if (IMinecraft.mc.player == null) return

        if (mode.get().equals("hypixel", true) && IMinecraft.mc.player!!.isOnGround && waitingPacket)
            IMinecraft.mc.options.jumpKey.isPressed = false
    }
}
