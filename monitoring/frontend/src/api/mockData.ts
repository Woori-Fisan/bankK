export const mockStats = [
    { label: 'Total TPS', value: '1,284', change: '+12%', trend: 'up' },
    { label: 'Error Rate', value: '0.02%', change: '-5%', trend: 'down' },
    { label: 'Avg Latency', value: '42ms', change: '+2ms', trend: 'up' },
    { label: 'Active Sessions', value: '8,421', change: '+423', trend: 'up' },
];

export const mockServiceStatus = [
    { name: 'Auth Service', status: 'Healthy', latency: '12ms', uptime: '99.99%' },
    { name: 'Transfer Service', status: 'Healthy', latency: '24ms', uptime: '99.95%' },
    { name: 'Account Service', status: 'Degraded', latency: '150ms', uptime: '98.2%' },
    { name: 'Notification Service', status: 'Healthy', latency: '8ms', uptime: '100%' },
];

export const mockAlerts = [
    { id: 1, type: 'Critical', message: 'Account Service latency > 100ms', time: '2 mins ago' },
    { id: 2, type: 'Warning', message: 'High memory usage on Node-04', time: '15 mins ago' },
    { id: 3, type: 'Info', message: 'Deployment of Auth-v2.1 success', time: '1 hour ago' },
];
