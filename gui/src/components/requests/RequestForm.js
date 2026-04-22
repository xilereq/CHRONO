// import React from 'react';
//
// const RequestForm = ({ type, title, setTitle, date, setDate, reason, setReason, onSubmit, onCancel }) => {
//     return (
//         <div className="section-panel form-top">
//             <h3 className="section-title">{type || "Nowy Wniosek"}</h3>
//             <form onSubmit={onSubmit} className="request-form">
//                 <label style={{display: 'block', marginBottom: '10px'}}>
//                     <div style={{marginBottom: '5px', fontSize: '0.9rem'}}>Tytuł</div>
//                     <input
//                         className="filter-input"
//                         style={{width: '100%'}}
//                         value={title}
//                         onChange={e => setTitle(e.target.value)}
//                         placeholder="np. Urlop wypoczynkowy"
//                     />
//                 </label>
//                 <label style={{display: 'block', marginBottom: '10px'}}>
//                     <div style={{marginBottom: '5px', fontSize: '0.9rem'}}>Data</div>
//                     <input
//                         className="filter-input"
//                         style={{width: '100%'}}
//                         value={date}
//                         onChange={e => setDate(e.target.value)}
//                         type="date"
//                     />
//                 </label>
//                 <label style={{display: 'block', marginBottom: '15px'}}>
//                     <div style={{marginBottom: '5px', fontSize: '0.9rem'}}>Powód (opcjonalnie)</div>
//                     <textarea
//                         className="filter-input"
//                         style={{width: '100%', fontFamily: 'inherit'}}
//                         value={reason}
//                         onChange={e => setReason(e.target.value)}
//                         rows={3}
//                     />
//                 </label>
//
//                 <div className="form-actions" style={{display: 'flex', gap: '10px'}}>
//                     <button className="big-yellow-btn" type="submit" style={{width: 'auto'}}>Wyślij wniosek</button>
//                     <button type="button" className="action-btn" style={{padding: '0 1rem'}} onClick={onCancel}>Anuluj</button>
//                 </div>
//             </form>
//         </div>
//     );
// };
//
// export default RequestForm;