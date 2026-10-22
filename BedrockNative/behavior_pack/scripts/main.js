// Bedrock-native diagnostic POC v0.0.1.
// No Fabric, Geyser, Java entities, teleporting, commands or cheats.
// First prove that Script API loads and vanilla Copper Golems can sort one item.
import { world, system } from "@minecraft/server";

const GOLEM = "minecraft:copper_golem";
const SOURCE_MARKER = "minecraft:stick";
const ALLOWED = new Set(["minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel"]);
const RADIUS = 5;
const TICK_INTERVAL = 60;
const MAX_GOLEMS = 6;
const cooldown = new Map();
let scanNumber = 0;

function blockContainer(dimension, x, y, z) {
  const block = dimension.getBlock({ x, y, z });
  if (!block || !ALLOWED.has(block.typeId)) return undefined;
  const inventory = block.getComponent("minecraft:inventory");
  if (!inventory?.container) return undefined;
  return { x, y, z, block, inventory: inventory.container };
}

function findChests(golem) {
  const d = golem.dimension;
  const at = golem.location;
  const cx = Math.floor(at.x), cy = Math.floor(at.y), cz = Math.floor(at.z);
  let source, destination;
  // Keep this intentionally small and deterministic for the first in-game test.
  for (let y = cy - 1; y <= cy + 1; y++) {
    for (let x = cx - RADIUS; x <= cx + RADIUS; x++) {
      for (let z = cz - RADIUS; z <= cz + RADIUS; z++) {
        const chest = blockContainer(d, x, y, z);
        if (!chest) continue;
        const sample = chest.inventory.getItem(0);
        if (!sample) continue;
        if (sample.typeId === SOURCE_MARKER) {
          if (!source) source = chest;
        } else if (!destination) {
          destination = chest;
        }
      }
    }
  }
  return { source, destination };
}

function firstTransfer(source, destination) {
  const src = source.inventory;
  const dst = destination.inventory;
  const sample = dst.getItem(0);
  if (!sample || sample.typeId === SOURCE_MARKER) return false;
  // Never consume the source marker or destination filter sample.
  for (let i = 1; i < src.size; i++) {
    const original = src.getItem(i);
    if (!original || original.typeId !== sample.typeId) continue;
    for (let j = 1; j < dst.size; j++) {
      const target = dst.getItem(j);
      if (target && (!target.isStackableWith(original) || target.amount >= target.maxAmount)) continue;
      const toMove = original.clone();
      toMove.amount = 1;
      const targetAfter = target ? target.clone() : toMove;
      if (target) targetAfter.amount += 1;
      const sourceAfter = original.amount === 1 ? undefined : original.clone();
      if (sourceAfter) sourceAfter.amount -= 1;
      // A failed destination write must not remove the item from its source.
      dst.setItem(j, targetAfter);
      try {
        src.setItem(i, sourceAfter);
      } catch (error) {
        dst.setItem(j, target);
        throw error;
      }
      return true;
    }
  }
  return false;
}

function notifyNearby(golem, message) {
  try {
    for (const player of golem.dimension.getPlayers({ location: golem.location, maxDistance: 16 })) {
      player.sendMessage(message);
    }
  } catch (error) {
    console.warn("[SCG Native] notification failed: " + error);
  }
}

console.warn("[SCG Native 0.0.1] JavaScript entry loaded.");
world.afterEvents.playerSpawn.subscribe(({ player, initialSpawn }) => {
  if (!initialSpawn) return;
  system.runTimeout(() => {
    try {
      if (player.isValid) {
        player.sendMessage("§a[SCG Native 0.0.1] Script Bedrock carregado! " +
          "Origem: graveto no primeiro espaço; destino: diamante no primeiro espaço.");
      }
    } catch (error) {
      console.warn("[SCG Native] greeting failed: " + error);
    }
  }, 30);
});

system.runInterval(() => {
  scanNumber++;
  for (const name of ["overworld", "nether", "the_end"]) {
    let golems;
    try {
      golems = world.getDimension(name).getEntities({ type: GOLEM });
    } catch (error) {
      console.warn("[SCG Native] dimension lookup failed: " + error);
      continue;
    }
    let inspected = 0;
    for (const golem of golems) {
      if (inspected++ >= MAX_GOLEMS) break;
      try {
        const until = cooldown.get(golem.id) ?? 0;
        if (scanNumber < until) continue;
        cooldown.set(golem.id, scanNumber + 2);
        const { source, destination } = findChests(golem);
        if (!source || !destination) continue;
        if (firstTransfer(source, destination)) {
          notifyNearby(golem, "§a[SCG Native] 1 item transferido com sucesso entre baús!");
          console.warn("[SCG Native] SUCCESS one item moved by golem " + golem.id);
        }
      } catch (error) {
        console.warn("[SCG Native] golem error: " + error);
        notifyNearby(golem, "§c[SCG Native] Erro no transporte; verifique o Content Log.");
      }
    }
  }
}, TICK_INTERVAL);
