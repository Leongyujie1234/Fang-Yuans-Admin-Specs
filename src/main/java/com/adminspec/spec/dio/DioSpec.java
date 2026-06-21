package com.adminspec.spec.dio;

import com.adminspec.moves.dio.DioBarrageMove;
import com.adminspec.moves.dio.DioChargeMove;
import com.adminspec.moves.dio.DioStandState;
import com.adminspec.moves.dio.DioTimeStopMove;
import com.adminspec.spec.Spec;
import com.adminspec.spec.SpecMove;
import com.adminspec.spec.SpecRegistry;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class DioSpec {
    private DioSpec() {}

    public static void register() {
        Spec spec = new Spec(
            "dio_the_world",
            Component.literal("DIO - The World"),
            Component.literal("The World stand from JoJo's Bizarre Adventure. Barrage, Charge, Time Stop, and throwable knives."),
            List.<SpecMove>of(
                new DioBarrageMove(),
                new DioChargeMove(),
                new DioTimeStopMove()
            )
        ) {
            @Override
            public void onRemoved(Player player) {
                super.onRemoved(player);
                DioStandState.removeStand(player);
            }
        };
        SpecRegistry.register(spec);
    }
}
