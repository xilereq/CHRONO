import React from 'react';
import { RequestList } from '../../components';
import { ApprovalList } from '../../pages';
import '../main.css'

const Requests = () => {
    return (
        <div className="page-container">
            <h2 className="page-title-no-margin">Moje wnioski</h2>
            <div>
                <RequestList />
                <ApprovalList />
            </div>
        </div>
    );
};

export default Requests;
