// Gráficos con Chart.js
document.addEventListener('DOMContentLoaded', () => {
  if (!window.security || !security.getToken) return;
  security.ensureAuthenticated();
  const ctx = document.getElementById('reportChart');
  if (!ctx) return;
  security.authFetch('/api/incidencias')
    .then(res => res.json())
    .then(data => {
      const counts = data.reduce((acc, inc) => {
        acc[inc.estado] = (acc[inc.estado] || 0) + 1;
        return acc;
      }, {});
      const labels = Object.keys(counts);
      const values = labels.map(l => counts[l]);
      new Chart(ctx.getContext('2d'), {
        type: 'bar',
        data: {
          labels: labels,
          datasets: [{
            label: 'Incidencias por estado',
            data: values,
            backgroundColor: 'rgba(54, 162, 235, 0.5)'
          }]
        },
        options: {
          scales: { y: { beginAtZero: true } }
        }
      });
    })
    .catch(err => console.error('Error al cargar datos de incidencias:', err));
});

