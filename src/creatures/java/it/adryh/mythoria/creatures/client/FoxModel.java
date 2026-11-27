package it.adryh.mythoria.creatures.client;

import it.adryh.mythoria.creatures.entity.SpiritFox;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Trotting legs, three swaying tails and a sitting pose. */
final class FoxModel extends HierarchicalModel<SpiritFox> {
    private static final float RAD = Mth.PI / 180.0F;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart[] tails;
    private final ModelPart rightFront;
    private final ModelPart leftFront;
    private final ModelPart rightHind;
    private final ModelPart leftHind;

    FoxModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        tails = new ModelPart[]{root.getChild("tail_c"), root.getChild("tail_r"), root.getChild("tail_l")};
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
    public void setupAnim(SpiritFox entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot += Mth.clamp(yaw, -60.0F, 60.0F) * RAD;
        head.xRot += pitch * RAD * 0.7F;

        float step = swing * 0.9F;
        float stride = Math.min(amount, 1.0F);
        rightFront.xRot += Mth.cos(step) * 1.3F * stride;
        leftFront.xRot -= Mth.cos(step) * 1.3F * stride;
        rightHind.xRot -= Mth.cos(step) * 1.3F * stride;
        leftHind.xRot += Mth.cos(step) * 1.3F * stride;

        for (int i = 0; i < tails.length; i++) {
            tails[i].yRot += Mth.sin(age * 0.11F + i * 2.1F) * 0.22F;
            tails[i].xRot += Mth.sin(age * 0.08F + i * 1.7F) * 0.14F;
        }

        if (entity.isInSittingPose()) {
            root.getChild("body").xRot = -0.35F;
            for (ModelPart part : new ModelPart[]{rightHind, leftHind}) {
                part.y += 3.5F;
                part.xRot = -1.4F;
            }
            for (ModelPart tail : tails) {
                tail.y += 3.0F;
                tail.xRot -= 0.45F;
            }
        }
    }
}
