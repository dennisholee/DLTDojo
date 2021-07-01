require('dotenv').config()

const { ClientBuilder } = require('@iota/client')
const crypto            = require('crypto')

async function run() {
  const client = new ClientBuilder()
      .node(process.env.API_ENDPOINT)
      .build()
  
  //------------------------------------------------------------------------------    
  // Get network info and check healthiness (isHealthy: true)
  //------------------------------------------------------------------------------    
  // client.getInfo().then(console.log).catch(console.error)

  //------------------------------------------------------------------------------    
  // Uncomment to generate mnemonic.
  // Add mnemoic to .env as follows MNEMONIC=${mnemonic}
  //------------------------------------------------------------------------------    
  //    const mnemonic = client.generateMnemonic()
  //    console.log(mnemonic)
  //     
  //    const hexEncodedSeed = client.mnemonicToHexSeed(mnemonic)

  const hexEncodedSeed = client.mnemonicToHexSeed(process.env.MNEMONIC)

  console.log(hexEncodedSeed)
  
  const addresses = await client.getAddresses(hexEncodedSeed)
    .accountIndex(0)
    .range(0,5)
    .get()

  //------------------------------------------------------------------------------    
  // Add tokens to the IOTA address : https://faucet.testnet.chrysalis2.com/
  //------------------------------------------------------------------------------

  addresses.forEach(async a => {
    console.log(a)
  })

  addresses.forEach(async a => {
    const outputs = await client.getAddressOutputs(a)

    outputs.forEach(async i => console.log(await client.getOutput(i)))
  })

  console.log(await client.getBalance(hexEncodedSeed))

  //------------------------------------------------------------------------------    
  // Uncomment to transfer tokens from one address to another address within same 
  // wallet.
  //------------------------------------------------------------------------------

  let min = 0
  let max = addresses.length - 1

  const idx = Math.floor(Math.random() * (max - min) + min)

  const message = await client.message()
    .seed(hexEncodedSeed)
    .output(addresses[idx], 1000000)
    .submit()

  console.log(message)
}
