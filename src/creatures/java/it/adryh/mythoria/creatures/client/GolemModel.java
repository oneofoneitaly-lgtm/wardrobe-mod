package it.adryh.mythoria.creatures.client;

import it.adryh.mythoria.creatures.entity.CrystalGolem;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Heavy walk, one-two punches and the two-fisted ground slam. */
final class GolemModel extends HierarchicalModel<CrystalGolem> {
    private static final float RAD = Mth.PI / 180.0F;

    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    GolemModel(ModelPart root) {
        this.root = root;
        torso = root.getChild("torso");
        head = torso.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(CrystalGolem entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float partial = Mth.clamp(age - entity.tickCount, 0.0F, 1.0F);
        head.yRot += Mth.clamp(yaw, -45.0F, 45.0F) * RAD;
        head.xRot += pitch * RAD * 0.5F;

        float step = swing * 0.45F;
        float stride = Math.min(amount, 1.0F);
        rightLeg.xRot += Mth.cos(step) * 0.9F * stride;
        leftLeg.xRot -= Mth.cos(step) * 0.9F * stride;
        rightArm.xRot -= Mth.cos(step) * 0.6F * stride;
        leftArm.xRot += Mth.cos(step) * 0.6F * stride;
        torso.yRot += Mth.sin(step) * 0.07F * stride;

        float breathe = Mth.sin(age * 0.06F);
        torso.y += breathe * 0.4F;
        rightArm.y += breathe * 0.6F;
        leftArm.y += breathe * 0.6F;
        rightArm.zRot += 0.05F + breathe * 0.03F;
        leftArm.zRot -= 0.05F + breathe * 0.03F;

        if (entity.slamTicks > 0) {
            // raise both fists, then bring them down
            float t = 26.0F - (entity.slamTicks - partial);
            float lift = t < 18.0F ? Mth.clamp(t / 14.0F, 0.0F, 1.0F) : Mth.clamp(1.0F - (t - 18.0F) / 2.0F, 0.0F, 1.0F);
            float hit = t >= 18.0F ? Mth.clamp((26.0F - t) / 8.0F, 0.0F, 1.0F) : 0.0F;
            rightArm.xRot = -2.8F * lift - 0.7F * hit;
            leftArm.xRot = rightArm.xRot;
            torso.xRot += -0.15F * lift + 0.3F * hit;
        } else if (entity.punchTicks > 0) {
            float t = Mth.sin(Mth.clamp((12.0F - (entity.punchTicks - partial)) / 12.0F, 0.0F, 1.0F) * Mth.PI);
            rightArm.xRot -= 2.0F * t;
            leftArm.xRot -= 1.3F * t;
        }
    }
}
