const samplePoliciesInit = [
    { id: 'policy-1', name: 'Polityka urlopowa', description: 'Maksymalnie 26 dni urlopu rocznie. Zasady zatwierdzania przez kierownika.' },
    { id: 'policy-2', name: 'Polityka pracy zdalnej', description: 'Możliwość pracy zdalnej do 2 dni w tygodniu po uzgodnieniu z przełożonym.' },
    { id: 'policy-3', name: 'Polityka bezpieczeństwa', description: 'Wymóg używania menedżera haseł i dwuskładnikowego uwierzytelniania dla systemów krytycznych.' }
];

let policiesStore = samplePoliciesInit.slice();

const delay = (ms = 200) => new Promise(resolve => setTimeout(resolve, ms));

const policiesService = {
    getAll: async () => {
        await delay();
        return policiesStore.slice(); // zwróć kopię
    },

    getById: async (id) => {
        await delay();
        const found = policiesStore.find(p => String(p.id) === String(id));
        if (!found) throw new Error('Polityka nie znaleziona');
        return { ...found };
    },

    create: async (policy) => {
        await delay();
        const newPolicy = { id: `local-${Date.now()}`, ...policy };
        policiesStore = [newPolicy, ...policiesStore];
        return { ...newPolicy };
    },

    update: async (id, policy) => {
        await delay();
        policiesStore = policiesStore.map(p => (String(p.id) === String(id) ? { ...p, ...policy } : p));
        const updated = policiesStore.find(p => String(p.id) === String(id));
        if (!updated) throw new Error('Polityka nie znaleziona');
        return { ...updated };
    },

    remove: async (id) => {
        await delay();
        const existed = policiesStore.some(p => String(p.id) === String(id));
        policiesStore = policiesStore.filter(p => String(p.id) !== String(id));
        return { success: existed };
    },

    // pomocnicze do testów: reset do sample
    _resetToSample: () => { policiesStore = samplePoliciesInit.slice(); }
};

export default policiesService;



