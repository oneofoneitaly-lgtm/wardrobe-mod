package it.adryh.mythoria.creatures.client;

import it.adryh.mythoria.creatures.entity.GoldenGriffin;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Animations: walking, sitting, folded wings on the ground and flapping in the air. */
final class GriffinModel extends HierarchicalModel<GoldenGriffin> {
    private static final float RAD = Mth.PI / 180.0F;

    private final ModelPart root;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart saddle;
    private final ModelPart collar;
    private final ModelPart tail;
    private final ModelPart tailMid;
    private final ModelPart rightWing;
    private final ModelPart rightTip;
    private final ModelPart leftWing;
    private final ModelPart leftTip;
    private final ModelPart rightFront;
    private final ModelPart leftFront;
    private final ModelPart rightHind;
    private final ModelPart leftHind;

    GriffinModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        saddle = body.getChild("saddle");
        collar = body.getChild("collar");
        neck = root.getChild("neck");
        head = neck.getChild("head");
        jaw = head.getChild("jaw");
        tail = root.getChild("tail");
        tailMid = tail.getChild("tail_mid");
        rightWing = root.getChild("r_wing");
        rightTip = rightWing.getChild("r_wing_tip");
        leftWing = root.getChild("l_wing");
        leftTip = leftWing.getChild("l_wing_tip");
        rightFront = root.getChild("right_front_leg");
        leftFront = root.getChild("left_front_leg");
        rightHind = root.getChild("right_hind_leg");
        leftHind = root.getChild("left_hind_leg");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(GoldenGriffin entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float partial = Mth.clamp(age - entity.tickCount, 0.0F, 1.0F);
        float fly = entity.flyAmount(partial);
        float ground = 1.0F - fly;
        boolean sitting = entity.isInSittingPose();

        saddle.visible = entity.isTame();
        collar.visible = entity.isTame();

        head.yRot += Mth.clamp(yaw, -50.0F, 50.0F) * RAD * 0.8F;
        head.xRot += pitch * RAD * 0.5F;
        neck.xRot += Mth.sin(age * 0.07F) * 0.03F;
        jaw.xRot += Math.max(0.0F, Mth.sin(age * 0.11F) - 0.92F) * 4.0F;

        tail.yRot += Mth.sin(age * 0.09F) * 0.25F;
        tailMid.yRot += Mth.sin(age * 0.09F - 0.8F) * 0.3F;

        float step = swing * 0.6662F;
        float stride = Math.min(amount, 1.0F) * ground;
        rightFront.xRot += Mth.cos(step) * 1.1F * stride;
        leftFront.xRot += Mth.cos(step + Mth.PI) * 1.1F * stride;
        rightHind.xRot += Mth.cos(step + Mth.PI) * 1.1F * stride;
        leftHind.xRot += Mth.cos(step) * 1.1F * stride;

        // in flight the legs tuck in
        rightFront.xRot -= 0.9F * fly;
        leftFront.xRot -= 0.9F * fly;
        rightHind.xRot += 0.9F * fly;
        leftHind.xRot += 0.9F * fly;

        // wings: lerp from folded along the flanks to flapping
        float time = entity.wingTime(partial);
        float flap = 0.15F + Mth.sin(time) * 0.65F;
        float tip = Mth.sin(time - 0.9F) * 0.5F;
        float breathe = Mth.sin(age * 0.08F) * 0.03F;
        rightWing.xRot = Mth.lerp(fly, -1.45F, 0.0F);
        rightWing.yRot = Mth.lerp(fly, 1.38F + breathe, -0.12F);
        rightWing.zRot = Mth.lerp(fly, 0.0F, flap);
        leftWing.xRot = Mth.lerp(fly, -1.45F, 0.0F);
        leftWing.yRot = Mth.lerp(fly, -1.38F - breathe, 0.12F);
        leftWing.zRot = Mth.lerp(fly, 0.0F, -flap);
        rightTip.zRot = tip * fly;
        leftTip.zRot = -tip * fly;
        rightTip.xScale = Mth.lerp(fly, 0.45F, 1.0F);
        leftTip.xScale = rightTip.xScale;
        rightWing.zScale = Mth.lerp(fly, 0.72F, 1.0F);
        leftWing.zScale = rightWing.zScale;

        if (sitting) {
            // lower the hind quarters and stretch the front legs
            for (ModelPart part : new ModelPart[]{rightHind, leftHind}) {
                part.y += 7.0F;
                part.xRot = -1.45F;
            }
            tail.y += 5.0F;
            tail.xRot -= 0.35F;
        }
    }
}
