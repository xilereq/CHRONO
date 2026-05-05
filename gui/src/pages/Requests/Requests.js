import React from 'react';
import { RequestList } from '../../components';

const Requests = () => {
    return (
        <div className="page-container">
            <h2 className="page-title-no-margin">Moje wnioski</h2>
            <RequestList />
        </div>
    );
};

export default Requests;
