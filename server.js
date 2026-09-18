const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = 3000;
const ROOT_DIR = __dirname;
const APK_PATH = path.join(ROOT_DIR, '.build-outputs', 'app-debug.apk');
const FALLBACK_APK_PATH = path.join(ROOT_DIR, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');

// In-memory data store for Hommie marketplace with realistic seed data
let marketplaceState = {
  jobs: [
    {
      id: 'JOB-801',
      title: 'Kitchen Sink Leak & Pipe Replacement',
      category: 'Plumbing',
      icon: 'wrench',
      customerName: 'Sarah Jenkins',
      customerAddress: '742 Evergreen Terrace, Springfield',
      customerPhone: '+1 (555) 382-9104',
      date: 'Today, Immediate',
      isUrgent: true,
      estimatedHours: 2.5,
      hourlyRate: 75,
      escrowAmount: 187.50,
      tipAmount: 0,
      status: 'in_progress', // 'pending', 'assigned', 'en_route', 'in_progress', 'completed', 'disputed'
      workerId: 'PRO-101',
      workerName: 'Marcus Vance',
      workerRating: 4.94,
      workerJobsCount: 142,
      workerPhone: '+1 (555) 901-4421',
      description: 'Main drain pipe under the sink has a hairline crack and is dripping onto the cabinet floor. Replacement PVC and seal needed.',
      createdAt: new Date(Date.now() - 45 * 60 * 1000).toISOString(),
      timeline: [
        { title: 'Job Posted & Escrow Locked', time: '45m ago', note: '$187.50 held securely in Hommie Escrow' },
        { title: 'Marcus Vance Accepted Gig', time: '40m ago', note: 'Worker dispatched from 1.2 miles away' },
        { title: 'Worker Arrived On-Site', time: '22m ago', note: 'Safety pin verified: #8491' },
        { title: 'Work In Progress', time: '18m ago', note: 'Pipes disassembled, installing replacement' }
      ]
    },
    {
      id: 'JOB-802',
      title: 'Full Deep Clean - 3 Bed / 2 Bath',
      category: 'Cleaning',
      icon: 'sparkles',
      customerName: 'David Chen',
      customerAddress: '1204 Pine Valley Rd, Apt 4B',
      customerPhone: '+1 (555) 234-8890',
      date: 'Tomorrow, 9:00 AM',
      isUrgent: false,
      estimatedHours: 4.0,
      hourlyRate: 50,
      escrowAmount: 200.00,
      tipAmount: 25,
      status: 'completed',
      workerId: 'PRO-102',
      workerName: 'Elena Rostova',
      workerRating: 4.98,
      workerJobsCount: 238,
      workerPhone: '+1 (555) 678-1123',
      description: 'Move-in deep scrub for kitchen appliances, bathrooms, hardwood floor waxing, and window interiors.',
      createdAt: new Date(Date.now() - 4 * 3600 * 1000).toISOString(),
      timeline: [
        { title: 'Job Created', time: '4h ago', note: '$200.00 locked in escrow' },
        { title: 'Elena Accepted', time: '3.5h ago', note: 'Scheduled pro' },
        { title: 'Work Completed', time: '30m ago', note: 'Before & After photos uploaded' },
        { title: 'Customer Approved Escrow', time: '10m ago', note: 'Payout released + $25 tip' }
      ]
    },
    {
      id: 'JOB-803',
      title: 'Ceiling Fan & Dimmer Switch Installation',
      category: 'Electrical',
      icon: 'zap',
      customerName: 'Rachel Green',
      customerAddress: '492 Maple Avenue, Unit 12',
      customerPhone: '+1 (555) 891-2345',
      date: 'Today, Within 2 Hours',
      isUrgent: true,
      estimatedHours: 2.0,
      hourlyRate: 85,
      escrowAmount: 170.00,
      tipAmount: 0,
      status: 'pending',
      workerId: null,
      workerName: null,
      workerRating: null,
      workerJobsCount: null,
      workerPhone: null,
      description: 'Replace existing light fixture with 52-inch Hunter ceiling fan and install a Lutron smart dimmer switch.',
      createdAt: new Date(Date.now() - 12 * 60 * 1000).toISOString(),
      timeline: [
        { title: 'Job Broadcasted to Nearby Pros', time: '12m ago', note: 'Escrow authorized ($170.00)' }
      ]
    },
    {
      id: 'JOB-804',
      title: 'Lawn Mowing & Shrub Trimming (0.25 Acre)',
      category: 'Yard Care',
      icon: 'trees',
      customerName: 'Robert Martinez',
      customerAddress: '88 Oakridge Boulevard',
      customerPhone: '+1 (555) 443-9090',
      date: 'Saturday, 10:00 AM',
      isUrgent: false,
      estimatedHours: 2.0,
      hourlyRate: 50,
      escrowAmount: 100.00,
      tipAmount: 0,
      status: 'pending',
      workerId: null,
      workerName: null,
      description: 'Front and backyard mowing, edge trimming along sidewalks, and blow cleanup.',
      createdAt: new Date(Date.now() - 25 * 60 * 1000).toISOString(),
      timeline: [
        { title: 'Job Broadcasted', time: '25m ago', note: '$100.00 held in Escrow' }
      ]
    }
  ],
  workers: [
    {
      id: 'PRO-101',
      name: 'Marcus Vance',
      skills: ['Plumbing', 'Handyman', 'AC Repair'],
      rating: 4.94,
      totalGigs: 142,
      isOnline: true,
      distanceMi: 0.9,
      todayEarnings: 285.00,
      walletBalance: 495.50
    },
    {
      id: 'PRO-102',
      name: 'Elena Rostova',
      skills: ['Cleaning', 'Painting', 'Organization'],
      rating: 4.98,
      totalGigs: 238,
      isOnline: true,
      distanceMi: 1.4,
      todayEarnings: 225.00,
      walletBalance: 610.00
    },
    {
      id: 'PRO-103',
      name: 'Jamal Harris',
      skills: ['Electrical', 'Smart Home', 'Appliance'],
      rating: 4.91,
      totalGigs: 98,
      isOnline: true,
      distanceMi: 2.1,
      todayEarnings: 170.00,
      walletBalance: 340.00
    }
  ]
};

function getApkInfo() {
  const targetPath = fs.existsSync(APK_PATH) ? APK_PATH : (fs.existsSync(FALLBACK_APK_PATH) ? FALLBACK_APK_PATH : null);
  if (!targetPath) {
    return { exists: false, sizeMb: 0, mtime: null, path: null };
  }
  const stat = fs.statSync(targetPath);
  return {
    exists: true,
    sizeBytes: stat.size,
    sizeMb: (stat.size / (1024 * 1024)).toFixed(2),
    mtime: stat.mtime.toISOString(),
    filename: 'hommie-debug.apk',
    version: '1.0.0 (Debug)'
  };
}

const HTML_CONTENT = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Hommie — Trusted Real-Time Local Services Gig Marketplace</title>
  <meta name="description" content="Official Hommie gig marketplace with customer booking, worker dispatching, secure escrow guarantees, and native Android APK support.">
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <style>
    body {
      font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background-color: #0F172A;
      color: #0F172A;
    }
    .badge-pulse {
      animation: pulse-ring 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
    }
    @keyframes pulse-ring {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.6; transform: scale(1.1); }
    }
    .custom-scrollbar::-webkit-scrollbar {
      width: 6px;
      height: 6px;
    }
    .custom-scrollbar::-webkit-scrollbar-thumb {
      background: #CBD5E1;
      border-radius: 9999px;
    }
    .custom-scrollbar::-webkit-scrollbar-track {
      background: transparent;
    }
  </style>
</head>
<body class="bg-slate-900 min-h-screen text-slate-800 antialiased flex flex-col items-center">

  <!-- Top Global Bar -->
  <header class="w-full bg-slate-900 border-b border-slate-800 text-white sticky top-0 z-50">
    <div class="max-w-7xl mx-auto px-4 py-3 flex flex-wrap items-center justify-between gap-3">
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 rounded-xl bg-indigo-600 flex items-center justify-center font-black text-white text-xl shadow-lg shadow-indigo-500/30">
          H
        </div>
        <div>
          <div class="flex items-center gap-2">
            <span class="font-extrabold text-lg tracking-tight">Hommie</span>
            <span class="bg-indigo-500/20 text-indigo-300 text-xs px-2 py-0.5 rounded-full font-medium border border-indigo-500/30">Local Gigs v1.0</span>
          </div>
          <p class="text-xs text-slate-400 hidden sm:block">Trusted, Escrow-Protected On-Demand Services Marketplace</p>
        </div>
      </div>

      <!-- Role Switcher Tabs -->
      <div class="flex items-center bg-slate-800/90 p-1 rounded-xl border border-slate-700/80 shadow-inner">
        <button id="tab-customer" onclick="setRole('customer')" class="px-3 py-1.5 rounded-lg text-xs sm:text-sm font-semibold transition-all flex items-center gap-1.5 bg-indigo-600 text-white shadow-sm">
          <span>👤</span>
          <span>Customer</span>
        </button>
        <button id="tab-worker" onclick="setRole('worker')" class="px-3 py-1.5 rounded-lg text-xs sm:text-sm font-semibold transition-all flex items-center gap-1.5 text-slate-300 hover:text-white">
          <span>👷</span>
          <span>Worker / Pro</span>
        </button>
        <button id="tab-admin" onclick="setRole('admin')" class="px-3 py-1.5 rounded-lg text-xs sm:text-sm font-semibold transition-all flex items-center gap-1.5 text-slate-300 hover:text-white">
          <span>🛡️</span>
          <span>Admin & Ops</span>
        </button>
      </div>

      <!-- Actions: Viewport Mode & APK Download -->
      <div class="flex items-center gap-2">
        <button id="btn-device-toggle" onclick="toggleDeviceFrame()" title="Toggle Phone Frame View" class="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium rounded-lg border border-slate-700 transition flex items-center gap-1.5">
          <span id="device-icon">📱</span>
          <span id="device-text" class="hidden md:inline">Mobile Frame</span>
        </button>
        <a href="/app-debug.apk" download="hommie-debug.apk" class="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold rounded-lg shadow-sm shadow-emerald-600/30 transition flex items-center gap-1.5">
          <svg class="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24"><path d="M17.523 15.3414c-.5511 0-.9993-.4486-.9993-1.0003 0-.551.4482-.9992.9993-.9992.5518 0 1.0007.4482 1.0007.9992 0 .5517-.4489 1.0003-1.0007 1.0003m-11.046 0c-.5511 0-.9993-.4486-.9993-1.0003 0-.551.4482-.9992.9993-.9992.5518 0 1.0007.4482 1.0007.9992 0 .5517-.4489 1.0003-1.0007 1.0003m11.4045-6.02l1.9973-3.4592a.416.416 0 00-.1521-.5676.416.416 0 00-.5676.1521l-2.0223 3.503C15.5802 8.4116 13.8443 8.1 12 8.1s-3.5802.3116-5.1368.8497L4.8409 5.4467a.4161.4161 0 00-.5677-.1521.4157.4157 0 00-.152 5676l1.9972 3.4592C2.6889 11.1867.3432 14.6589 0 18.7778h24c-.3432-4.1189-2.6889-7.5911-6.1185-9.4564"/></svg>
          <span>APK (22.8MB)</span>
        </a>
      </div>
    </div>
  </header>

  <!-- Main Container (Responsive or Mobile Phone Shell) -->
  <main id="app-shell" class="w-full flex-1 flex justify-center py-4 px-2 sm:px-4">
    <!-- Frame Wrapper -->
    <div id="phone-frame" class="w-full max-w-[420px] bg-slate-100 rounded-[36px] shadow-2xl border-4 border-slate-800 overflow-hidden flex flex-col transition-all duration-300 relative min-h-[780px]">
      
      <!-- Phone Notch / Status Bar -->
      <div id="phone-notch" class="bg-slate-900 text-white px-6 pt-2 pb-1.5 flex items-center justify-between text-xs font-semibold select-none">
        <span id="live-clock">9:41</span>
        <div class="w-20 h-4 bg-black rounded-full mx-auto"></div>
        <div class="flex items-center gap-1.5 text-[11px]">
          <span>5G</span>
          <span>100%</span>
        </div>
      </div>

      <!-- Scrollable Phone Content -->
      <div id="screen-container" class="flex-1 overflow-y-auto custom-scrollbar bg-slate-50 flex flex-col">

        <!-- =================================================================== -->
        <!-- VIEW 1: CUSTOMER VIEW -->
        <!-- =================================================================== -->
        <section id="view-customer" class="flex flex-col flex-1 p-4 space-y-4">
          
          <!-- Escrow Trust Guarantee Header -->
          <div class="bg-gradient-to-r from-indigo-700 to-indigo-900 text-white rounded-2xl p-4 shadow-md shadow-indigo-900/20 relative overflow-hidden">
            <div class="relative z-10">
              <div class="flex items-center justify-between mb-1">
                <span class="text-xs uppercase tracking-wider font-bold text-indigo-200">🛡️ Hommie Escrow Shield</span>
                <span class="text-xs bg-emerald-500/20 text-emerald-300 font-semibold px-2 py-0.5 rounded-full border border-emerald-500/30">100% Covered</span>
              </div>
              <h1 class="text-lg font-bold">Book Local Gigs Safely</h1>
              <p class="text-xs text-indigo-100/90 mt-1 leading-relaxed">Payment is held safely in escrow. Pros are paid only when you inspect and approve the work.</p>
            </div>
            <div class="absolute -right-6 -bottom-6 w-24 h-24 bg-indigo-500/20 rounded-full blur-xl pointer-events-none"></div>
          </div>

          <!-- Active Job Live Tracking Widget (if in progress) -->
          <div id="customer-active-tracker" class="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm space-y-3">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="w-2.5 h-2.5 rounded-full bg-emerald-500 badge-pulse"></span>
                <span class="text-xs font-bold text-slate-900 uppercase tracking-wide">Live Gig Status</span>
              </div>
              <span id="tracker-job-id" class="text-xs font-mono bg-slate-100 px-2 py-0.5 rounded text-slate-600">JOB-801</span>
            </div>

            <div class="border-t border-slate-100 pt-2">
              <h2 id="tracker-title" class="font-bold text-sm text-slate-900">Kitchen Sink Leak & Pipe Replacement</h2>
              <p id="tracker-desc" class="text-xs text-slate-500 mt-0.5">Assigned Pro: <strong class="text-slate-800 font-semibold" id="tracker-pro">Marcus Vance (4.94⭐)</strong></p>
            </div>

            <!-- Progress Stepper -->
            <div class="py-2">
              <div class="flex items-center justify-between text-[11px] font-semibold text-slate-600 mb-2">
                <span class="text-indigo-600">Requested</span>
                <span class="text-indigo-600">Assigned</span>
                <span id="step-active-label" class="text-indigo-600 font-bold">In Progress</span>
                <span class="text-slate-400">Complete</span>
              </div>
              <div class="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
                <div id="tracker-progress-bar" class="bg-indigo-600 h-full rounded-full transition-all duration-500" style="width: 75%;"></div>
              </div>
            </div>

            <!-- Escrow Status Pill -->
            <div class="flex items-center justify-between bg-indigo-50/80 rounded-xl p-2.5 border border-indigo-100">
              <div class="flex items-center gap-2">
                <span class="text-indigo-700 text-sm">🔒</span>
                <div>
                  <div class="text-xs font-bold text-indigo-950">Escrow Locked: <span id="tracker-escrow">$187.50</span></div>
                  <div class="text-[10px] text-indigo-700">Release upon customer satisfaction</div>
                </div>
              </div>
              <button onclick="openReleaseModal()" id="btn-approve-escrow" class="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-lg transition shadow-sm">
                Approve & Pay
              </button>
            </div>

            <!-- Pro Contact Bar -->
            <div class="flex items-center justify-between pt-1 text-xs">
              <div class="flex items-center gap-2">
                <div class="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 font-bold flex items-center justify-center text-xs">
                  MV
                </div>
                <div>
                  <div class="font-bold text-slate-800">Marcus Vance</div>
                  <div class="text-[11px] text-slate-500">Arrived 18 mins ago</div>
                </div>
              </div>
              <div class="flex gap-1.5">
                <button onclick="simulateCall()" class="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold rounded-lg text-xs transition">
                  📞 Call
                </button>
                <button onclick="simulateChat()" class="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold rounded-lg text-xs transition">
                  💬 Chat
                </button>
              </div>
            </div>
          </div>

          <!-- Service Category Quick Grid -->
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <h2 class="font-bold text-sm text-slate-900">What do you need help with?</h2>
              <span class="text-xs text-indigo-600 font-semibold">8 Categories</span>
            </div>
            
            <div class="grid grid-cols-2 gap-2.5">
              <button onclick="selectService('Plumbing', 75, 'wrench')" class="service-card p-3 bg-white hover:bg-indigo-50/50 border border-slate-200 hover:border-indigo-300 rounded-xl text-left transition flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center text-lg">🔧</div>
                <div>
                  <div class="font-bold text-xs text-slate-900">Plumbing</div>
                  <div class="text-[11px] text-slate-500">$75/hr • Emergency</div>
                </div>
              </button>

              <button onclick="selectService('Cleaning', 50, 'sparkles')" class="service-card p-3 bg-white hover:bg-indigo-50/50 border border-slate-200 hover:border-indigo-300 rounded-xl text-left transition flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center text-lg">🧹</div>
                <div>
                  <div class="font-bold text-xs text-slate-900">Deep Cleaning</div>
                  <div class="text-[11px] text-slate-500">$50/hr • Standard</div>
                </div>
              </button>

              <button onclick="selectService('Electrical', 85, 'zap')" class="service-card p-3 bg-white hover:bg-indigo-50/50 border border-slate-200 hover:border-indigo-300 rounded-xl text-left transition flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center text-lg">⚡</div>
                <div>
                  <div class="font-bold text-xs text-slate-900">Electrical</div>
                  <div class="text-[11px] text-slate-500">$85/hr • Certified</div>
                </div>
              </button>

              <button onclick="selectService('Handyman', 55, 'hammer')" class="service-card p-3 bg-white hover:bg-indigo-50/50 border border-slate-200 hover:border-indigo-300 rounded-xl text-left transition flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center text-lg">🔨</div>
                <div>
                  <div class="font-bold text-xs text-slate-900">Handyman</div>
                  <div class="text-[11px] text-slate-500">$55/hr • Quick Fix</div>
                </div>
              </button>
            </div>
          </div>

          <!-- Quick Request Booking Form -->
          <div class="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm space-y-3">
            <h2 class="font-bold text-sm text-slate-900 flex items-center gap-1.5">
              <span>📝</span>
              <span>Post a New Gig</span>
            </h2>
            
            <form id="job-request-form" onsubmit="handleCreateJob(event)" class="space-y-3">
              <div>
                <label class="block text-xs font-semibold text-slate-700 mb-1">Service & Issue Summary</label>
                <input id="form-job-title" type="text" required placeholder="e.g., Leaking water heater, Drywall patch" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none">
              </div>

              <div class="grid grid-cols-2 gap-2">
                <div>
                  <label class="block text-xs font-semibold text-slate-700 mb-1">Category</label>
                  <select id="form-category" onchange="updateHourlyEstimate()" class="w-full px-2.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none">
                    <option value="Plumbing">Plumbing ($75/h)</option>
                    <option value="Cleaning">Cleaning ($50/h)</option>
                    <option value="Electrical">Electrical ($85/h)</option>
                    <option value="Handyman">Handyman ($55/h)</option>
                    <option value="Yard Care">Yard Care ($50/h)</option>
                  </select>
                </div>
                <div>
                  <label class="block text-xs font-semibold text-slate-700 mb-1">Est. Duration</label>
                  <select id="form-hours" onchange="updateHourlyEstimate()" class="w-full px-2.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none">
                    <option value="1">1 Hour</option>
                    <option value="2" selected>2 Hours</option>
                    <option value="3">3 Hours</option>
                    <option value="4">4 Hours</option>
                  </select>
                </div>
              </div>

              <div>
                <label class="block text-xs font-semibold text-slate-700 mb-1">Your Location Address</label>
                <input id="form-address" type="text" required value="742 Evergreen Terrace, Springfield" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none">
              </div>

              <!-- Urgent Dispatch Switch -->
              <div class="flex items-center justify-between p-2.5 bg-amber-50 rounded-xl border border-amber-200/80">
                <div class="flex items-center gap-2">
                  <span class="text-base">⚡</span>
                  <div>
                    <div class="text-xs font-bold text-amber-900">Urgent Dispatch</div>
                    <div class="text-[10px] text-amber-700">Need pro within 2 hours</div>
                  </div>
                </div>
                <input id="form-urgent" type="checkbox" checked class="w-4 h-4 text-indigo-600 rounded">
              </div>

              <!-- Escrow Total Preview Box -->
              <div class="p-3 bg-slate-900 text-white rounded-xl flex items-center justify-between">
                <div>
                  <div class="text-[10px] text-slate-400 font-medium">Secured Escrow Deposit</div>
                  <div id="form-escrow-display" class="text-base font-extrabold text-white">$150.00</div>
                </div>
                <button type="submit" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold rounded-lg transition shadow-md shadow-indigo-600/30">
                  Lock & Dispatch 🚀
                </button>
              </div>
            </form>
          </div>

        </section>

        <!-- =================================================================== -->
        <!-- VIEW 2: WORKER / PRO VIEW -->
        <!-- =================================================================== -->
        <section id="view-worker" class="flex flex-col flex-1 p-4 space-y-4 hidden">
          
          <!-- Worker Profile & Availability Header -->
          <div class="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div class="w-12 h-12 rounded-2xl bg-indigo-600 text-white font-bold flex items-center justify-center text-base shadow-sm">
                MV
              </div>
              <div>
                <div class="flex items-center gap-1.5">
                  <h2 class="font-bold text-sm text-slate-900">Marcus Vance</h2>
                  <span class="text-xs text-amber-500 font-bold">★ 4.94</span>
                </div>
                <div class="text-xs text-slate-500">142 Gigs • Master Plumber</div>
              </div>
            </div>
            <!-- Online Toggle -->
            <div class="flex flex-col items-end">
              <button id="worker-online-btn" onclick="toggleWorkerOnline()" class="px-3 py-1.5 bg-emerald-100 text-emerald-800 text-xs font-bold rounded-xl border border-emerald-300 flex items-center gap-1.5 transition">
                <span class="w-2 h-2 rounded-full bg-emerald-600"></span>
                <span id="worker-online-text">ONLINE</span>
              </button>
            </div>
          </div>

          <!-- Worker Wallet Summary -->
          <div class="grid grid-cols-2 gap-2.5">
            <div class="bg-indigo-600 text-white p-3.5 rounded-2xl shadow-sm">
              <div class="text-xs text-indigo-200 font-medium">Today's Earnings</div>
              <div id="worker-today-earnings" class="text-xl font-black mt-0.5">$285.00</div>
              <div class="text-[10px] text-indigo-200 mt-1">2 gigs completed today</div>
            </div>
            <div class="bg-slate-900 text-white p-3.5 rounded-2xl shadow-sm flex flex-col justify-between">
              <div>
                <div class="text-xs text-slate-400 font-medium">Available Balance</div>
                <div id="worker-wallet-balance" class="text-xl font-black mt-0.5">$495.50</div>
              </div>
              <button onclick="simulateCashout()" class="mt-2 w-full py-1 bg-emerald-600 hover:bg-emerald-500 text-white text-[11px] font-bold rounded-lg transition">
                Instant Cashout
              </button>
            </div>
          </div>

          <!-- Active Accepted Gig Progress Controls -->
          <div class="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm space-y-3">
            <div class="flex items-center justify-between">
              <h3 class="font-bold text-xs uppercase tracking-wider text-indigo-700">Active Gig Underway</h3>
              <span class="text-xs font-bold px-2 py-0.5 bg-amber-100 text-amber-800 rounded-md">JOB-801</span>
            </div>

            <div>
              <div class="font-bold text-sm text-slate-900">Kitchen Sink Leak & Pipe Replacement</div>
              <div class="text-xs text-slate-600 mt-0.5">Customer: Sarah Jenkins • 742 Evergreen Terrace</div>
              <div class="text-xs text-emerald-700 font-bold mt-1">Escrow Payout: $187.50</div>
            </div>

            <!-- Pro State Transition Buttons -->
            <div class="space-y-1.5 pt-1">
              <div class="text-xs font-semibold text-slate-700">Update Job Step:</div>
              <div class="grid grid-cols-2 gap-2">
                <button onclick="updateJobStatus('JOB-801', 'en_route')" class="p-2 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-semibold rounded-xl text-center transition">
                  🚗 On The Way
                </button>
                <button onclick="updateJobStatus('JOB-801', 'in_progress')" class="p-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-800 text-xs font-semibold rounded-xl text-center border border-indigo-200 transition">
                  🔧 Start Work
                </button>
              </div>
              <button onclick="updateJobStatus('JOB-801', 'completed')" class="w-full py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl transition shadow-sm">
                📸 Upload Proof & Request Escrow Payout
              </button>
            </div>
          </div>

          <!-- Nearby Incoming Gigs Feed -->
          <div class="space-y-2.5">
            <div class="flex items-center justify-between">
              <h3 class="font-bold text-sm text-slate-900">Available Gigs Nearby (Radar)</h3>
              <span id="available-gigs-count" class="text-xs bg-slate-200 text-slate-700 font-bold px-2 py-0.5 rounded-full">2 Jobs</span>
            </div>

            <div id="worker-jobs-list" class="space-y-2.5">
              <!-- Dynamically rendered jobs -->
            </div>
          </div>

        </section>

        <!-- =================================================================== -->
        <!-- VIEW 3: ADMIN & OPS VIEW -->
        <!-- =================================================================== -->
        <section id="view-admin" class="flex flex-col flex-1 p-4 space-y-4 hidden">
          
          <div class="bg-slate-900 text-white rounded-2xl p-4 shadow-sm">
            <div class="text-xs text-slate-400 font-semibold uppercase tracking-wider">Hommie Platform Overview</div>
            <div class="grid grid-cols-2 gap-3 mt-3">
              <div class="bg-slate-800/80 p-2.5 rounded-xl border border-slate-700">
                <div class="text-[11px] text-slate-400">Total Escrow Held</div>
                <div id="admin-escrow-total" class="text-lg font-black text-emerald-400">$657.50</div>
              </div>
              <div class="bg-slate-800/80 p-2.5 rounded-xl border border-slate-700">
                <div class="text-[11px] text-slate-400">Active Live Gigs</div>
                <div id="admin-active-count" class="text-lg font-black text-indigo-400">4 Gigs</div>
              </div>
              <div class="bg-slate-800/80 p-2.5 rounded-xl border border-slate-700">
                <div class="text-[11px] text-slate-400">Verified Pros Online</div>
                <div class="text-lg font-black text-slate-200">14 Pros</div>
              </div>
              <div class="bg-slate-800/80 p-2.5 rounded-xl border border-slate-700">
                <div class="text-[11px] text-slate-400">Dispute Rate</div>
                <div class="text-lg font-black text-amber-400">0.0% (Zero)</div>
              </div>
            </div>
          </div>

          <!-- Android APK Artifact Status Card -->
          <div class="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm space-y-3">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="text-lg">🤖</span>
                <span class="text-xs font-bold text-slate-900 uppercase">Native Android APK</span>
              </div>
              <span class="text-[11px] bg-emerald-100 text-emerald-800 font-bold px-2 py-0.5 rounded-full">Compiled</span>
            </div>

            <div class="text-xs text-slate-600 space-y-1 bg-slate-50 p-3 rounded-xl border border-slate-200 font-mono text-[11px]">
              <div>Artifact: <strong class="text-slate-800">app-debug.apk</strong></div>
              <div>Size: <strong id="apk-size">22.8 MB</strong></div>
              <div>Target SDK: <strong>36 (Android 16)</strong></div>
              <div>Architecture: <strong>Kotlin + Jetpack Compose</strong></div>
            </div>

            <a href="/app-debug.apk" download="hommie-debug.apk" class="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl transition flex items-center justify-center gap-2 shadow-sm">
              <span>📥 Download Native Android APK</span>
            </a>
          </div>

          <!-- All Marketplace Transactions Audit List -->
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <h3 class="font-bold text-sm text-slate-900">Marketplace Transactions</h3>
              <button onclick="refreshData()" class="text-xs text-indigo-600 font-semibold hover:underline">Refresh</button>
            </div>

            <div id="admin-transactions-list" class="space-y-2">
              <!-- Rendered via JS -->
            </div>
          </div>

        </section>

      </div>

      <!-- Phone Bottom Home Bar Indicator -->
      <div class="bg-white py-1.5 flex justify-center border-t border-slate-100">
        <div class="w-28 h-1 bg-slate-300 rounded-full"></div>
      </div>

    </div>
  </main>

  <!-- Modal: Escrow Approval & Rating -->
  <div id="modal-escrow" class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 hidden">
    <div class="bg-white w-full max-w-sm rounded-3xl p-5 space-y-4 shadow-2xl">
      <div class="text-center">
        <div class="w-12 h-12 rounded-full bg-emerald-100 text-emerald-600 text-2xl flex items-center justify-center mx-auto mb-2">
          ✓
        </div>
        <h3 class="font-bold text-base text-slate-900">Approve & Release Escrow</h3>
        <p class="text-xs text-slate-500 mt-1">Funds will be immediately credited to Marcus Vance's wallet.</p>
      </div>

      <div class="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-xs space-y-1.5">
        <div class="flex justify-between">
          <span class="text-slate-500">Escrow Amount:</span>
          <span class="font-bold text-slate-900">$187.50</span>
        </div>
        <div class="flex justify-between">
          <span class="text-slate-500">Add Optional Tip:</span>
          <div class="flex gap-1">
            <button onclick="setTip(5)" class="px-2 py-0.5 bg-white border border-slate-200 rounded text-xs font-semibold hover:bg-indigo-50 hover:text-indigo-600">$5</button>
            <button onclick="setTip(10)" class="px-2 py-0.5 bg-white border border-slate-200 rounded text-xs font-semibold hover:bg-indigo-50 hover:text-indigo-600">$10</button>
            <button onclick="setTip(15)" class="px-2 py-0.5 bg-white border border-slate-200 rounded text-xs font-semibold hover:bg-indigo-50 hover:text-indigo-600">$15</button>
          </div>
        </div>
      </div>

      <div class="space-y-1.5">
        <label class="block text-xs font-semibold text-slate-700">Rate Your Pro</label>
        <div class="flex justify-center gap-2 text-2xl text-amber-400 cursor-pointer">
          <span>★</span><span>★</span><span>★</span><span>★</span><span>★</span>
        </div>
      </div>

      <div class="flex gap-2">
        <button onclick="closeReleaseModal()" class="flex-1 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition">
          Cancel
        </button>
        <button onclick="confirmReleaseEscrow()" class="flex-1 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl transition shadow-md">
          Release $187.50
        </button>
      </div>
    </div>
  </div>

  <script>
    let currentRole = 'customer';
    let isPhoneFrame = true;
    let currentTip = 0;
    let jobs = [];

    // Initialize clock
    function updateClock() {
      const now = new Date();
      let hours = now.getHours();
      let minutes = now.getMinutes();
      minutes = minutes < 10 ? '0' + minutes : minutes;
      document.getElementById('live-clock').innerText = hours + ':' + minutes;
    }
    setInterval(updateClock, 1000);
    updateClock();

    function setRole(role) {
      currentRole = role;
      document.getElementById('view-customer').classList.toggle('hidden', role !== 'customer');
      document.getElementById('view-worker').classList.toggle('hidden', role !== 'worker');
      document.getElementById('view-admin').classList.toggle('hidden', role !== 'admin');

      const tabs = ['customer', 'worker', 'admin'];
      tabs.forEach(t => {
        const btn = document.getElementById('tab-' + t);
        if (t === role) {
          btn.className = 'px-3 py-1.5 rounded-lg text-xs sm:text-sm font-semibold transition-all flex items-center gap-1.5 bg-indigo-600 text-white shadow-sm';
        } else {
          btn.className = 'px-3 py-1.5 rounded-lg text-xs sm:text-sm font-semibold transition-all flex items-center gap-1.5 text-slate-300 hover:text-white';
        }
      });

      if (role === 'worker' || role === 'admin') {
        fetchJobs();
      }
    }

    function toggleDeviceFrame() {
      isPhoneFrame = !isPhoneFrame;
      const frame = document.getElementById('phone-frame');
      const notch = document.getElementById('phone-notch');
      const icon = document.getElementById('device-icon');
      const text = document.getElementById('device-text');

      if (isPhoneFrame) {
        frame.className = 'w-full max-w-[420px] bg-slate-100 rounded-[36px] shadow-2xl border-4 border-slate-800 overflow-hidden flex flex-col transition-all duration-300 relative min-h-[780px]';
        notch.classList.remove('hidden');
        icon.innerText = '📱';
        text.innerText = 'Mobile Frame';
      } else {
        frame.className = 'w-full max-w-4xl bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden flex flex-col transition-all duration-300 relative min-h-[780px]';
        notch.classList.add('hidden');
        icon.innerText = '💻';
        text.innerText = 'Desktop View';
      }
    }

    function selectService(category, rate, icon) {
      document.getElementById('form-category').value = category;
      updateHourlyEstimate();
      document.getElementById('form-job-title').focus();
    }

    function updateHourlyEstimate() {
      const cat = document.getElementById('form-category').value;
      const hours = parseFloat(document.getElementById('form-hours').value || '2');
      let rate = 75;
      if (cat === 'Cleaning') rate = 50;
      if (cat === 'Electrical') rate = 85;
      if (cat === 'Handyman') rate = 55;
      if (cat === 'Yard Care') rate = 50;

      const total = rate * hours;
      document.getElementById('form-escrow-display').innerText = '$' + total.toFixed(2);
    }

    async function fetchJobs() {
      try {
        const res = await fetch('/api/jobs');
        const data = await res.json();
        jobs = data.jobs;
        renderWorkerGigs();
        renderAdminTransactions();
      } catch (err) {
        console.error('Failed to fetch jobs:', err);
      }
    }

    function renderWorkerGigs() {
      const list = document.getElementById('worker-jobs-list');
      const countPill = document.getElementById('available-gigs-count');
      const pendingJobs = jobs.filter(j => j.status === 'pending');
      
      countPill.innerText = pendingJobs.length + ' Jobs';

      if (pendingJobs.length === 0) {
        list.innerHTML = '<div class="bg-white p-4 rounded-xl border border-slate-200 text-center text-xs text-slate-500">No open gigs nearby right now. You are ready for alerts!</div>';
        return;
      }

      list.innerHTML = pendingJobs.map(job => \`
        <div class="bg-white rounded-2xl p-3.5 border border-slate-200 shadow-sm space-y-2.5">
          <div class="flex items-start justify-between">
            <div>
              <span class="text-[10px] font-bold px-2 py-0.5 rounded bg-indigo-50 text-indigo-700 uppercase">\${job.category}</span>
              <h4 class="font-bold text-xs text-slate-900 mt-1">\${job.title}</h4>
              <p class="text-[11px] text-slate-500 mt-0.5">\${job.customerAddress}</p>
            </div>
            <div class="text-right">
              <div class="text-sm font-black text-emerald-600">$\${job.escrowAmount.toFixed(2)}</div>
              <div class="text-[10px] text-slate-400">Est. \${job.estimatedHours} hrs</div>
            </div>
          </div>
          <div class="flex items-center justify-between pt-1 border-t border-slate-100 text-xs">
            <span class="text-[11px] text-slate-500">Client: \${job.customerName}</span>
            <div class="flex gap-1.5">
              <button onclick="handleAcceptJob('\${job.id}')" class="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white font-bold rounded-lg text-xs transition">
                Accept Gig
              </button>
            </div>
          </div>
        </div>
      \`).join('');
    }

    function renderAdminTransactions() {
      const list = document.getElementById('admin-transactions-list');
      if (!list) return;

      let totalEscrow = 0;
      jobs.forEach(j => totalEscrow += (j.escrowAmount || 0));
      document.getElementById('admin-escrow-total').innerText = '$' + totalEscrow.toFixed(2);
      document.getElementById('admin-active-count').innerText = jobs.length + ' Gigs';

      list.innerHTML = jobs.map(j => \`
        <div class="bg-white p-3 rounded-xl border border-slate-200 text-xs flex items-center justify-between">
          <div>
            <div class="font-bold text-slate-900">\${j.id} • \${j.title}</div>
            <div class="text-[11px] text-slate-500">Client: \${j.customerName} | Status: <strong class="uppercase text-indigo-700">\${j.status}</strong></div>
          </div>
          <div class="text-right">
            <div class="font-bold text-emerald-600">$\${j.escrowAmount.toFixed(2)}</div>
            <div class="text-[10px] text-slate-400">Escrow Protected</div>
          </div>
        </div>
      \`).join('');
    }

    async function handleCreateJob(e) {
      e.preventDefault();
      const title = document.getElementById('form-job-title').value;
      const category = document.getElementById('form-category').value;
      const hours = parseFloat(document.getElementById('form-hours').value);
      const address = document.getElementById('form-address').value;
      const isUrgent = document.getElementById('form-urgent').checked;

      let rate = 75;
      if (category === 'Cleaning') rate = 50;
      if (category === 'Electrical') rate = 85;
      if (category === 'Handyman') rate = 55;
      if (category === 'Yard Care') rate = 50;

      const payload = {
        title,
        category,
        estimatedHours: hours,
        hourlyRate: rate,
        customerAddress: address,
        isUrgent
      };

      try {
        const res = await fetch('/api/jobs', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        const data = await res.json();
        alert('🎉 Job posted successfully! $' + (rate * hours).toFixed(2) + ' held in Escrow and dispatched to nearby pros.');
        document.getElementById('job-request-form').reset();
        fetchJobs();
      } catch (err) {
        alert('Failed to post job');
      }
    }

    async function handleAcceptJob(jobId) {
      try {
        const res = await fetch('/api/jobs/' + jobId + '/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ action: 'accept', workerId: 'PRO-101' })
        });
        alert('✅ You accepted job ' + jobId + '! Head over to Active Gigs.');
        fetchJobs();
      } catch (err) {
        alert('Error accepting job');
      }
    }

    async function updateJobStatus(jobId, status) {
      try {
        const res = await fetch('/api/jobs/' + jobId + '/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ action: status })
        });
        alert('Step updated to: ' + status.replace('_', ' ').toUpperCase());
        fetchJobs();
      } catch (err) {
        alert('Failed to update status');
      }
    }

    function toggleWorkerOnline() {
      const btn = document.getElementById('worker-online-btn');
      const txt = document.getElementById('worker-online-text');
      if (txt.innerText === 'ONLINE') {
        txt.innerText = 'OFFLINE';
        btn.className = 'px-3 py-1.5 bg-slate-100 text-slate-600 text-xs font-bold rounded-xl border border-slate-300 flex items-center gap-1.5 transition';
      } else {
        txt.innerText = 'ONLINE';
        btn.className = 'px-3 py-1.5 bg-emerald-100 text-emerald-800 text-xs font-bold rounded-xl border border-emerald-300 flex items-center gap-1.5 transition';
      }
    }

    function openReleaseModal() {
      document.getElementById('modal-escrow').classList.remove('hidden');
    }

    function closeReleaseModal() {
      document.getElementById('modal-escrow').classList.add('hidden');
    }

    function setTip(amt) {
      currentTip = amt;
    }

    async function confirmReleaseEscrow() {
      try {
        await fetch('/api/jobs/JOB-801/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ action: 'approve_escrow', tip: currentTip })
        });
        closeReleaseModal();
        alert('🎉 Escrow payout of $187.50 + $' + currentTip + ' tip released to Marcus Vance! Thank you for choosing Hommie.');
        document.getElementById('tracker-progress-bar').style.width = '100%';
        document.getElementById('step-active-label').innerText = 'Completed & Paid';
        document.getElementById('step-active-label').className = 'text-emerald-600 font-bold';
        document.getElementById('btn-approve-escrow').innerText = 'Released ✓';
        document.getElementById('btn-approve-escrow').disabled = true;
        fetchJobs();
      } catch (err) {
        alert('Failed to release escrow');
      }
    }

    function simulateCall() {
      alert('📞 Connecting to Marcus Vance via Hommie Masked Number (+1 555-901-4421)...');
    }

    function simulateChat() {
      alert('💬 Chat with Marcus Vance:\\n"Marcus: Hi Sarah, I\\'m almost done testing the cold water line under the sink."');
    }

    function simulateCashout() {
      alert('💸 Transfer of $495.50 initiated to Chase Bank account ending in •••• 4019. Available in 30 minutes.');
    }

    function refreshData() {
      fetchJobs();
    }

    // Initial load
    fetchJobs();
  </script>
</body>
</html>
`;

const server = http.createServer((req, res) => {
  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = parsedUrl.pathname;

  // Set standard security & CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // Health endpoint
  if (pathname === '/health' || pathname === '/api/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', app: 'Hommie', port: PORT, timestamp: new Date().toISOString() }));
    return;
  }

  // Serve Android APK file
  if (pathname === '/app-debug.apk' || pathname === '/download/apk') {
    const targetPath = fs.existsSync(APK_PATH) ? APK_PATH : (fs.existsSync(FALLBACK_APK_PATH) ? FALLBACK_APK_PATH : null);
    if (!targetPath) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('APK not yet compiled');
      return;
    }
    const stat = fs.statSync(targetPath);
    res.writeHead(200, {
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Length': stat.size,
      'Content-Disposition': 'attachment; filename="hommie-debug.apk"'
    });
    fs.createReadStream(targetPath).pipe(res);
    return;
  }

  // API: Get APK Metadata
  if (pathname === '/api/apk-info') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(getApkInfo()));
    return;
  }

  // API: Get Jobs
  if (pathname === '/api/jobs' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ jobs: marketplaceState.jobs }));
    return;
  }

  // API: Create Job
  if (pathname === '/api/jobs' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        const data = JSON.parse(body || '{}');
        const newId = 'JOB-' + (800 + marketplaceState.jobs.length + 1);
        const hours = parseFloat(data.estimatedHours || 2);
        const rate = parseFloat(data.hourlyRate || 75);
        const newJob = {
          id: newId,
          title: data.title || 'General Home Service',
          category: data.category || 'Handyman',
          customerName: data.customerName || 'CurrentUser',
          customerAddress: data.customerAddress || '123 Main Street',
          date: data.isUrgent ? 'Today, Urgent' : 'Scheduled',
          isUrgent: !!data.isUrgent,
          estimatedHours: hours,
          hourlyRate: rate,
          escrowAmount: hours * rate,
          tipAmount: 0,
          status: 'pending',
          workerId: null,
          workerName: null,
          createdAt: new Date().toISOString(),
          timeline: [
            { title: 'Job Broadcasted', time: 'Just now', note: '$' + (hours * rate).toFixed(2) + ' held in Escrow' }
          ]
        };
        marketplaceState.jobs.unshift(newJob);
        res.writeHead(201, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, job: newJob }));
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Invalid JSON payload' }));
      }
    });
    return;
  }

  // API: Job Actions (accept, status update, approve escrow)
  if (pathname.startsWith('/api/jobs/') && pathname.endsWith('/action') && req.method === 'POST') {
    const parts = pathname.split('/');
    const jobId = parts[3];
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        const payload = JSON.parse(body || '{}');
        const job = marketplaceState.jobs.find(j => j.id === jobId);
        if (!job) {
          res.writeHead(404, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Job not found' }));
          return;
        }

        if (payload.action === 'accept') {
          job.status = 'assigned';
          job.workerId = payload.workerId || 'PRO-101';
          job.workerName = 'Marcus Vance';
          job.timeline.push({ title: 'Accepted by Marcus Vance', time: 'Just now', note: 'Worker en route' });
        } else if (payload.action === 'en_route') {
          job.status = 'en_route';
          job.timeline.push({ title: 'Worker En Route', time: 'Just now', note: 'Approaching location' });
        } else if (payload.action === 'in_progress') {
          job.status = 'in_progress';
          job.timeline.push({ title: 'Work In Progress', time: 'Just now', note: 'Work underway' });
        } else if (payload.action === 'completed') {
          job.status = 'completed';
          job.timeline.push({ title: 'Pro Marked Completed', time: 'Just now', note: 'Awaiting customer inspection' });
        } else if (payload.action === 'approve_escrow') {
          job.status = 'paid';
          job.tipAmount = payload.tip || 0;
          job.timeline.push({ title: 'Customer Released Escrow', time: 'Just now', note: 'Funds released to Pro' });
        }

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, job }));
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to process action' }));
      }
    });
    return;
  }

  // Fallback / Main SPA Page
  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(HTML_CONTENT);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[Hommie Dev Server] Running and listening on http://0.0.0.0:${PORT}`);
});
