/*
 * Smart Copper Golems Extended — Bedrock offline alpha
 * Standalone Script API implementation; no Java mod/Geyser required.
 * Respects survival crafting of vanilla copper golems, substitutes custom visual
 * golem on spawn, and moves stacks between explicitly filtered nearby chests.
 * Alpha limits: chest-slot filters, straight-line collision-aware movement;
 * item frame scanning and full Java AI parity are not claimed.
 */
import { world, system } from "@minecraft/server";
import { CATEGORIES } from "./category-index.js";

const SMART = "scg:smart_copper_golem";
const VANILLA = "minecraft:copper_golem";
const HORIZONTAL_RADIUS = 9;
const VERTICAL_RADIUS = 5;
const INTERVAL = 2;
const SCAN_COOLDOWN = 80;
const WALK_TIMEOUT = 200;
const ANIMATION_TICKS = 52; // 5.05 seconds played at roughly 2x speed
const MAX_CARRY = 16;
const SOURCE_MARKER = "minecraft:stick";
const CATEGORY_MARKER = "minecraft:paper";
const COPPER_CHEST_SUFFIX = "_copper_chest";
const TARGET_CONTAINERS = new Set(["minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel"]);
const PASSABLE = new Set([
  "minecraft:air", "minecraft:short_grass", "minecraft:tall_grass",
  "minecraft:fern", "minecraft:dead_bush", "minecraft:torch",
  "minecraft:redstone_torch", "minecraft:wall_torch",
  "minecraft:snow_layer", "minecraft:vine",
]);
const sessions = new Map();
const reserved = new Set();
let counter = 0;

function key(dim, p) {
  return dim.id + ":" + p.x + "," + p.y + "," + p.z;
}
function sameType(a, b) {
  return !!a && !!b && a.typeId === b.typeId;
}
function containerAt(dim, p) {
  const block = dim.getBlock(p);
  if (!block) return undefined;
  const inv = block.getComponent("minecraft:inventory");
  if (!inv?.container) return undefined;
  return { block, container: inv.container, pos: p };
}
function isCopperChest(typeId) {
  return typeId === "minecraft:copper_chest" || typeId.endsWith(COPPER_CHEST_SUFFIX);
}
function isSource(c) {
  return isCopperChest(c.block.typeId) ||
    (TARGET_CONTAINERS.has(c.block.typeId) && c.container.getItem(0)?.typeId === SOURCE_MARKER);
}
function firstSourceSlot(c) {
  return isCopperChest(c.block.typeId) ? 0 : 1;
}
function isDestination(c) {
  return TARGET_CONTAINERS.has(c.block.typeId) &&
    !isSource(c) && !!c.container.getItem(0);
}
function matchesFilter(item, dest) {
  const sample = dest.container.getItem(0);
  if (!item || !sample) return false;
  if (dest.container.getItem(1)?.typeId !== CATEGORY_MARKER) {
    // Exact means item identifier, not stack count or custom enchantments.
    return sameType(item, sample);
  }
  const left = CATEGORIES[item.typeId], right = CATEGORIES[sample.typeId];
  return !!left && left === right;
}
function findSlot(dest, item) {
  const inv = dest.container;
  // Slot 0 is always the filter sample; slot 1 selects exact or category.
  for (let i = 2; i < inv.size; i++) {
    const existing = inv.getItem(i);
    if (existing && existing.isStackableWith(item) && existing.amount < existing.maxAmount) {
      return { slot: i, limit: existing.maxAmount - existing.amount };
    }
  }
  for (let i = 2; i < inv.size; i++) {
    if (!inv.getItem(i)) return { slot: i, limit: item.maxAmount };
  }
  return undefined;
}
function scanNearby(mob) {
  const dim = mob.dimension, at = mob.location;
  const centerX = Math.floor(at.x), centerY = Math.floor(at.y), centerZ = Math.floor(at.z);
  const sources = [], destinations = [];
  for (let y = centerY - VERTICAL_RADIUS; y <= centerY + VERTICAL_RADIUS; y++) {
    for (let x = centerX - HORIZONTAL_RADIUS; x <= centerX + HORIZONTAL_RADIUS; x++) {
      for (let z = centerZ - HORIZONTAL_RADIUS; z <= centerZ + HORIZONTAL_RADIUS; z++) {
        const c = containerAt(dim, { x, y, z });
        if (!c) continue;
        if (isSource(c)) sources.push(c);
        else if (isDestination(c)) destinations.push(c);
      }
    }
  }
  return { sources, destinations };
}
function horizontalDistSq(a, b) {
  return (a.x - b.x) ** 2 + (a.z - b.z) ** 2;
}
function chooseTask(mob) {
  const { sources, destinations } = scanNearby(mob);
  let choice, best = Infinity;
  for (const source of sources) {
    if (Math.abs(source.pos.y - mob.location.y) > 2) continue;
    const sourceKey = key(mob.dimension, source.pos);
    if (reserved.has(sourceKey)) continue;
    for (let index = firstSourceSlot(source); index < source.container.size; index++) {
      const item = source.container.getItem(index);
      if (!item || (index === 0 && item.typeId === SOURCE_MARKER)) continue;
      for (const dest of destinations) {
        const destKey = key(mob.dimension, dest.pos);
        if (destKey === sourceKey || reserved.has(destKey) || !matchesFilter(item, dest)) continue;
        const slot = findSlot(dest, item);
        if (!slot) continue;
        const height = dest.pos.y - Math.floor(mob.location.y);
        if (height > 5 || height < -1) continue;
        const cost = horizontalDistSq(mob.location, source.pos) +
          horizontalDistSq(source.pos, dest.pos) + Math.abs(height) * 3;
        if (cost < best) {
          best = cost;
          choice = { source: source.pos, dest: dest.pos, sourceSlot: index,
            itemType: item.typeId, sourceKey, destKey };
        }
      }
    }
  }
  return choice;
}
function release(session) {
  if (session.task) {
    reserved.delete(session.task.sourceKey);
    reserved.delete(session.task.destKey);
  }
  session.task = undefined;
}
function reset(mob, session) {
  try { mob.setProperty("scg:reach", 0); } catch { /* entity may unload */ }
  try { mob.setProperty("minecraft:chest_interaction", "none"); } catch { /* optional */ }
  release(session);
  session.phase = "idle";
  session.age = 0;
  session.cooldown = SCAN_COOLDOWN;
}
function animate(mob, progress) {
  mob.setProperty("scg:reach", Math.max(0, Math.min(1, progress)));
}
function interactPose(mob, state) {
  try { mob.setProperty("minecraft:chest_interaction", state); } catch { /* vanilla fallback */ }
}
function freeSpace(dim, x, y, z) {
  const p = { x: Math.floor(x), y: Math.floor(y), z: Math.floor(z) };
  const legs = dim.getBlock(p);
  const head = dim.getBlock({ x: p.x, y: p.y + 1, z: p.z });
  const ground = dim.getBlock({ x: p.x, y: p.y - 1, z: p.z });
  if (!legs || !head || !ground || ground.isAir || ground.isLiquid) return false;
  return (legs.isAir || PASSABLE.has(legs.typeId)) &&
    (head.isAir || PASSABLE.has(head.typeId));
}
function moveToward(mob, target) {
  const p = mob.location;
  const dx = target.x + 0.5 - p.x, dz = target.z + 0.5 - p.z;
  const distance = Math.hypot(dx, dz);
  if (distance < 1.6 && Math.abs(p.y - target.y) < 6) return true;
  if (distance < 0.001) return false;
  const step = Math.min(0.28, distance);
  const x = p.x + dx / distance * step, z = p.z + dz / distance * step;
  // No clipping through walls or teleporting through an inaccessible floor.
  if (!freeSpace(mob.dimension, x, p.y, z)) return false;
  return mob.tryTeleport({ x, y: p.y, z }, {
    facingLocation: { x: target.x + 0.5, y: p.y + 0.7, z: target.z + 0.5 }
  });
}
function turnTo(mob, target) {
  const p = mob.location;
  const dx = target.x + 0.5 - p.x, dz = target.z + 0.5 - p.z;
  if (Math.abs(dx) + Math.abs(dz) < 0.005) return;
  mob.setRotation({ x: 0, y: Math.atan2(-dx, dz) * 180 / Math.PI });
}
function commitTransfer(mob, task) {
  const dim = mob.dimension;
  const source = containerAt(dim, task.source), dest = containerAt(dim, task.dest);
  if (!source || !dest || !isSource(source) || !isDestination(dest)) return false;
  const current = source.container.getItem(task.sourceSlot);
  if (!current || current.typeId !== task.itemType || !matchesFilter(current, dest)) return false;
  const slot = findSlot(dest, current);
  if (!slot || slot.limit <= 0) return false;
  const n = Math.min(MAX_CARRY, current.amount, slot.limit);
  if (n <= 0) return false;
  const previousTarget = dest.container.getItem(slot.slot);
  const transferred = current.clone();
  transferred.amount = n;
  const remaining = current.amount - n;
  const nextSource = remaining ? current.clone() : undefined;
  if (nextSource) nextSource.amount = remaining;
  const nextTarget = previousTarget ? previousTarget.clone() : transferred;
  if (previousTarget) nextTarget.amount += n;
  // Synchronous API operations. Verify everything before changing either side;
  // restore the original source if the target write fails.
  source.container.setItem(task.sourceSlot, nextSource);
  try {
    dest.container.setItem(slot.slot, nextTarget);
  } catch (error) {
    source.container.setItem(task.sourceSlot, current);
    throw error;
  }
  return true;
}
function newSession() {
  return { phase: "idle", age: 0, cooldown: 0, task: undefined,
    reachMax: 0, growAge: 0 };
}
function update(mob) {
  const id = mob.id;
  let s = sessions.get(id);
  if (!s) { s = newSession(); sessions.set(id, s); }
  s.age += INTERVAL;
  if (s.phase === "idle") {
    if ((s.cooldown -= INTERVAL) > 0) return;
    s.cooldown = SCAN_COOLDOWN;
    const task = chooseTask(mob);
    if (!task) return;
    reserved.add(task.sourceKey);
    reserved.add(task.destKey);
    s.task = task;
    s.phase = "source";
    s.age = 0;
  }
  if (!s.task) return;
  const task = s.task;
  if (s.phase === "source" || s.phase === "destination") {
    if (s.age > WALK_TIMEOUT) { reset(mob,s); return; }
    const pos = s.phase === "source" ? task.source : task.dest;
    const arrived = moveToward(mob, pos);
    if (!arrived) return;
    if (s.phase === "source") {
      s.phase = "pickup";
      s.age = 0;
      interactPose(mob, "take");
      return;
    }
    // Move to a high chest: use the same max reach ratio as the Java mod.
    const height = task.dest.y - Math.floor(mob.location.y);
    s.reachMax = height >= 2 && height <= 5 ? (height - 1) / 4 : 0;
    s.phase = s.reachMax ? "grow" : "put";
    s.age = 0;
    return;
  }
  if (s.phase === "pickup" && s.age >= 10) {
    interactPose(mob, "none");
    s.phase = "destination";
    s.age = 0;
    return;
  }
  if (s.phase === "grow") {
    turnTo(mob, task.dest);
    animate(mob, s.reachMax * Math.min(1, s.age / ANIMATION_TICKS));
    if (s.age >= ANIMATION_TICKS) {
      s.phase = "put"; s.age = 0;
    }
    return;
  }
  if (s.phase === "put") {
    turnTo(mob, task.dest);
    interactPose(mob, "put");
    if (s.age < 14) return;
    try { commitTransfer(mob, task); }
    catch (error) { console.warn("[SCG Offline] Transfer failed: " + error); }
    interactPose(mob, "none");
    s.phase = s.reachMax ? "shrink" : "done";
    s.age = 0;
    return;
  }
  if (s.phase === "shrink") {
    turnTo(mob, task.dest);
    animate(mob, s.reachMax * (1 - Math.min(1, s.age / ANIMATION_TICKS)));
    if (s.age >= ANIMATION_TICKS) reset(mob, s);
    return;
  }
  if (s.phase === "done") reset(mob, s);
}

// Vanilla copper golems created in survival become the standalone smart entity.
// Existing loaded copper golems are NOT force-converted; that protects old worlds.
world.afterEvents.entitySpawn.subscribe(({ entity }) => {
  if (entity.typeId !== VANILLA) return;
  system.run(() => {
    try {
      if (!entity.isValid) return;
      const replacement = entity.dimension.spawnEntity(SMART, entity.location);
      const oxidation = entity.getProperty("minecraft:oxidation_level");
      if (typeof oxidation === "string") replacement.setProperty("minecraft:oxidation_level", oxidation);
      entity.remove();
    } catch (error) {
      console.warn("[SCG Offline] Could not replace vanilla golem: " + error);
    }
  });
});

system.runInterval(() => {
  counter++;
  if (counter % (SCAN_COOLDOWN * 5 / INTERVAL) === 0) {
    // Release reservations of dead/unloaded golems and re-scan lazily.
    const active = new Set();
    for (const name of ["minecraft:overworld", "minecraft:nether", "minecraft:the_end"]) {
      try { for (const g of world.getDimension(name).getEntities({ type: SMART })) active.add(g.id); }
      catch { /* dimension not available */ }
    }
    for (const [id, session] of sessions) {
      if (!active.has(id)) { release(session); sessions.delete(id); }
    }
  }
  for (const name of ["minecraft:overworld", "minecraft:nether", "minecraft:the_end"]) {
    try {
      const dimension = world.getDimension(name);
      for (const golem of dimension.getEntities({ type: SMART })) {
        try { update(golem); }
        catch (error) {
          console.warn("[SCG Offline] Golem " + golem.id + ": " + error);
          const session = sessions.get(golem.id);
          if (session) reset(golem, session);
        }
      }
    } catch (error) {
      console.warn("[SCG Offline] dimension " + name + ": " + error);
    }
  }
}, INTERVAL);
