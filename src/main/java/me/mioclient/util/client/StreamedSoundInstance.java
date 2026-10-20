package me.mioclient.util.client;

import me.mioclient.api.ClientAccessor;

import java.io.InputStream;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.Identifier;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.phys.Vec3;

public class StreamedSoundInstance extends AbstractSoundInstance {
    public InputStream inputStream;

    public StreamedSoundInstance(Vec3 vec3d, InputStream inputStream) {
        super(Identifier.fromNamespaceAndPath(ClientAccessor.isGlobalsLoaded() ? "mioloader" : "mio", "sound"), SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.x = vec3d.x;
        this.y = vec3d.y;
        this.z = vec3d.z;
        this.inputStream = inputStream;
    }

    public StreamedSoundInstance(InputStream inputStream) {
        super(Identifier.fromNamespaceAndPath(ClientAccessor.isGlobalsLoaded() ? "mioloader" : "mio", "sound"), SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.inputStream = inputStream;
        this.relative = true;
        this.x = 0.0d;
        this.y = 0.0d;
        this.z = (-1.0d);
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        WeighedSoundEvents events = new WeighedSoundEvents(getIdentifier(), null);
        events.addSound(new Sound(getIdentifier(), ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true, false, 16));
        this.sound = events.getSound(this.random);
        return events;
    }
}
