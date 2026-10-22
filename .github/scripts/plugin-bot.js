// A player with the mod, for plugin-smoke.py: joins, says hello, throws a stack, dies with another one in
// the inventory and prints, as one JSON line, every despawn clock and death-loot timer the plugin sent.
// It does not register the clocks channel first, as the mod does on Spigot, so the plugin has to add it.
//
//   node plugin-bot.js <port> <minecraft version>
const mineflayer = require('mineflayer')

const [port, version] = process.argv.slice(2)
const bot = mineflayer.createBot({ host: '127.0.0.1', port: +port, username: 'smoke', version, auth: 'offline' })
const out = { clocks: [], loot: [], errors: [] }
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))

function varInt (buf, pos) {
  let value = 0
  let shift = 0
  let b
  do {
    b = buf[pos.i++]
    value |= (b & 0x7f) << shift
    shift += 7
  } while (b & 0x80)
  return value >>> 0
}
const zigZag = v => (v >>> 1) ^ -(v & 1)

// The two messages of common/src/main/java/com/kasper/despawnindicator/{ClockMessage,LootMessage}.java
bot._client.on('custom_payload', packet => {
  if (packet.channel !== 'despawnindicator:clocks') return
  const d = packet.data
  const pos = { i: 1 }
  if (d[0] === 1) {
    const clocks = []
    for (let n = varInt(d, pos); n > 0; n--) {
      const id = varInt(d, pos) | 0
      const flags = d[pos.i++]
      const clock = { id, flags }
      if (!(flags & 1)) {
        clock.remaining = zigZag(varInt(d, pos))
        clock.lifespan = varInt(d, pos)
      }
      clocks.push(clock)
    }
    out.clocks.push(clocks)
  } else if (d[0] === 2) {
    if (d[1] === 0) {
      out.loot.push({ present: false })
      return
    }
    pos.i = 2
    const length = varInt(d, pos)
    const dimension = d.slice(pos.i, pos.i + length).toString()
    pos.i += length
    const loot = { present: true, dimension }
    loot.x = zigZag(varInt(d, pos))
    loot.y = zigZag(varInt(d, pos))
    loot.z = zigZag(varInt(d, pos))
    loot.items = varInt(d, pos)
    loot.flags = d[pos.i++]
    if (!(loot.flags & 1)) loot.remaining = zigZag(varInt(d, pos))
    out.loot.push(loot)
  } else {
    out.errors.push('unknown message ' + d[0])
  }
})

let done = false
function finish () {
  if (done) return
  done = true
  console.log('RESULT ' + JSON.stringify(out))
  try { bot.quit() } catch (e) {}
  setTimeout(() => process.exit(0), 500)
}
bot.on('kicked', reason => { out.errors.push('kicked: ' + JSON.stringify(reason)); finish() })
bot.on('error', e => out.errors.push('error: ' + e.message))

bot.once('spawn', async () => {
  try {
    // HelloMessage: protocol 2 (clocks and death loot), then the mod's version
    const name = Buffer.from('smoke-test')
    bot._client.write('custom_payload', {
      channel: 'despawnindicator:hello',
      data: Buffer.concat([Buffer.from([2, name.length]), name])
    })
    await sleep(3000)
    bot.chat('/give @s minecraft:stone 5')
    await sleep(1500)
    const stone = bot.inventory.items().find(i => i.name === 'stone')
    if (stone) await bot.tossStack(stone)
    else out.errors.push('the server gave no stone')
    await sleep(3000)
    bot.chat('/give @s minecraft:dirt 3')
    await sleep(1500)
    bot.chat('/kill @s')
    await sleep(4000)
    // plugin-smoke.py runs /despawnindicator now, while the bot is still online
    console.log('READY')
    await sleep(3000)
  } catch (e) {
    out.errors.push('step: ' + e.message)
  }
  finish()
})
setTimeout(() => { out.errors.push('timed out'); finish() }, 90000)
