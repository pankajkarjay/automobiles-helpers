const VEHICLE_SERVICE_UUID = '0000abcd-0000-1000-8000-00805f9b34fb';
const VEHICLE_COMMAND_CHARACTERISTIC_UUID = '0000dcba-0000-1000-8000-00805f9b34fb';

const statusEl = document.getElementById('connectionStatus');
const connectBtn = document.getElementById('connectBtn');
const disconnectBtn = document.getElementById('disconnectBtn');
const logEl = document.getElementById('log');
const actionButtons = [...document.querySelectorAll('.action')];

let device;
let gattServer;
let commandCharacteristic;

function appendLog(message) {
  const li = document.createElement('li');
  li.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
  logEl.prepend(li);
}

function setConnectedState(isConnected) {
  statusEl.textContent = isConnected ? `Connected: ${device?.name ?? 'Vehicle'}` : 'Disconnected';
  connectBtn.disabled = isConnected;
  disconnectBtn.disabled = !isConnected;
  actionButtons.forEach((button) => {
    button.disabled = !isConnected;
  });
}

async function connectVehicle() {
  if (!('bluetooth' in navigator)) {
    appendLog('Web Bluetooth is unavailable in this browser.');
    return;
  }

  try {
    appendLog('Requesting authorized Bluetooth vehicle...');
    device = await navigator.bluetooth.requestDevice({
      filters: [{ services: [VEHICLE_SERVICE_UUID] }],
      optionalServices: [VEHICLE_SERVICE_UUID],
    });

    device.addEventListener('gattserverdisconnected', () => {
      appendLog('Vehicle disconnected.');
      setConnectedState(false);
    });

    gattServer = await device.gatt.connect();
    const service = await gattServer.getPrimaryService(VEHICLE_SERVICE_UUID);
    commandCharacteristic = await service.getCharacteristic(VEHICLE_COMMAND_CHARACTERISTIC_UUID);

    setConnectedState(true);
    appendLog(`Connected to ${device.name || 'authorized vehicle'}.`);
  } catch (error) {
    appendLog(`Connection failed: ${error.message}`);
    setConnectedState(false);
  }
}

async function disconnectVehicle() {
  if (device?.gatt?.connected) {
    device.gatt.disconnect();
  }

  commandCharacteristic = undefined;
  gattServer = undefined;
  setConnectedState(false);
  appendLog('Disconnected by user.');
}

async function sendCommand(command) {
  if (!commandCharacteristic) {
    appendLog('Cannot send command: not connected.');
    return;
  }

  try {
    const payload = new TextEncoder().encode(command);
    await commandCharacteristic.writeValue(payload);
    appendLog(`Command sent: ${command}`);
  } catch (error) {
    appendLog(`Command failed (${command}): ${error.message}`);
  }
}

connectBtn.addEventListener('click', connectVehicle);
disconnectBtn.addEventListener('click', disconnectVehicle);
actionButtons.forEach((button) => {
  button.addEventListener('click', () => {
    sendCommand(button.dataset.command);
  });
});

if ('serviceWorker' in navigator) {
  window.addEventListener('load', async () => {
    try {
      await navigator.serviceWorker.register('sw.js');
      appendLog('Service worker registered for installability.');
    } catch (error) {
      appendLog(`Service worker registration failed: ${error.message}`);
    }
  });
}
