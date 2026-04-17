document.addEventListener('DOMContentLoaded', () => {

    const ctx = document.getElementById('timeSeriesDelayChart').getContext('2d');
    let chartInstance = null;

    document.getElementById('syncTriggerBtn').addEventListener('click', () => {
        document.getElementById('syncStatus').innerText = "Status: Syncing...";
        fetch('/api/sync', { method: 'POST' })
            .then(res => res.json())
            .then(data => {
                document.getElementById('syncStatus').innerText = "Status: " + data.status;
                loadData();
            });
    });

    function loadData() {
        fetch('/api/data')
            .then(res => res.json())
            .then(logs => {
                
                // Calculates the average
                if(logs.length > 0) {
                    const totalDelay = logs.reduce((sum, log) => sum + log.responseDelaySeconds, 0);
                    const avg = totalDelay / logs.length;
                    document.getElementById('live_average_time').innerText = Math.round(avg) + " Seconds";
                }

                const chartData = logs.map(log => ({
                    x: new Date(log.timestamp).getTime(),
                    y: log.responseDelaySeconds,
                    incidentId: log.incidentId,
                    type: log.incidentType
                })).sort((a,b) => a.x - b.x);

                renderChart(chartData);
            });
    }

    function renderChart(data) {
        if (chartInstance) {
            chartInstance.destroy();
        }

        chartInstance = new Chart(ctx, {
            type: 'scatter',
            data: {
                datasets: [{
                    label: 'Response Delay (Seconds)',
                    data: data,
                    backgroundColor: 'rgba(239, 68, 68, 0.6)',
                    borderColor: 'rgba(239, 68, 68, 1)',
                    pointRadius: 6,
                    pointHoverRadius: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: {
                        type: 'linear',
                        position: 'bottom',
                        title: {
                            display: true,
                            text: 'Timeline'
                        },
                        ticks: {
                            callback: function(val) {
                                return new Date(val).toLocaleTimeString();
                            }
                        }
                    },
                    y: {
                        title: {
                            display: true,
                            text: 'Delay Gap (Seconds)'
                        },
                        beginAtZero: true
                    }
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            label: function(ctx) {
                                const payload = ctx.raw;
                                return `[${payload.type}] ID:${payload.incidentId} Delay:${payload.y}s`;
                            }
                        }
                    }
                }
            }
        });
    }

    // Initial load
    loadData();
});
